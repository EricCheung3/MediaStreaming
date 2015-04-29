package net.majorkernelpanic.streaming.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;

import junit.framework.Assert;
import net.majorkernelpanic.streaming.rtsp.RtspClient.Response;
import android.os.Handler;

public class RtpThread extends Thread {

	private static final int READ_QUEUE_CAPACITY = 20;
	public static final int WHAT_THREAD_END_UNEXCEPTION = RtpThread.class.hashCode();
	private String mHost;
	private int mPort;
	private boolean mStoped = false;

	/**
	 * Store the data which after reading（after flip）
	 */
	private ArrayBlockingQueue<ByteBuffer> mReadQueue = new ArrayBlockingQueue<ByteBuffer>(READ_QUEUE_CAPACITY);
	private ArrayBlockingQueue<ByteBuffer> mWriteQueue = new ArrayBlockingQueue<ByteBuffer>(50);
	private ArrayBlockingQueue<ByteBuffer> mCacheQueue = new ArrayBlockingQueue<ByteBuffer>(100);

	private Handler mHandler;

	public RtpThread(String host, int port) {
		super(String.format("RTPThread[%s:%d]", host, port));
		mHost = host;
		mPort = port;
	}

	public Response requestWithResponse(String request) throws InterruptedException {
		ByteBuffer buffer = available(request.getBytes().length);
		buffer.put(request.getBytes());
		buffer.flip();
		commitWrite(buffer);
		int i = 0;
		StringBuffer sb = new StringBuffer();
		do {
			buffer = mReadQueue.take();
			i = buffer.remaining() - 1;
			boolean found = false;
			for (; i > 2;) {
				int tp = i;
				if (buffer.get(tp--) == '\n' && buffer.get(tp--) == '\r' && buffer.get(tp--) == '\n' && buffer.get(tp--) == '\r') {
					found = true;
					break;
				}
				i = tp;
			}
			if (!found) {
				i = buffer.remaining();
			} else {
				i++;
			}
			if (found) {
				for (int j = 0; j < i; j++) {
					sb.append((char) buffer.get(j));
				}

				break;
			}
		} while (!mStoped);
		if (mStoped) {
			return null;
		}
		String content = sb.toString();
		Response response = new Response();
		Matcher matcher;
		matcher = Response.regexStatus.matcher(content);
		matcher.find();
		response.status = Integer.parseInt(matcher.group(1));
		if (response.status == 200) {
			String[] lines = content.split("\r\n");
			for (i = 1; i < lines.length; i++) {
				String line = lines[i];
				// Parsing headers of the request
				if (line.length() > 3) {
					matcher = Response.rexegHeader.matcher(line);
					matcher.find();
					response.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2));
				} else {
					break;
				}
			}
		}
		return response;
	}

	@Override
	public void run() {
		super.run();
		SocketChannel sc = null;
		Selector selector = null;
		try {
			final InetAddress[] inetAddress = new InetAddress[1];
			Thread t = new Thread(RtpThread.this, "getByName") {

				@Override
				public void run() {
					try {
						inetAddress[0] = InetAddress.getByName(mHost);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			};

			t.setDaemon(true);
			t.start();
			Thread.yield();
			t.join();
			if (mStoped || inetAddress[0] == null) {
				return;
			}
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(inetAddress[0].getHostAddress(), mPort));
			while (!mStoped && !sc.finishConnect()) {
				Thread.sleep(3);
			}

			selector = Selector.open();
			sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			while (!mStoped) {
				int num = selector.select();
				if (num > 0) {
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (!mStoped && it.hasNext()) {
						SelectionKey selectionKey = it.next();
						if (selectionKey.isReadable()) {
							SocketChannel channel = (SocketChannel) selectionKey.channel();
							if (doRead(channel) < 0) {
								mStoped = true;
							}
						}
						if (!mStoped) {
							if (selectionKey.isWritable()) {
								SocketChannel channel = (SocketChannel) selectionKey.channel();
								if (doWrite(channel) < 0) {
									mStoped = true;
								}
							} else {
//								Log.d(getName(), "selector is not writable");
							}
						}
						it.remove();
					}
					//TODO the original code is Thread.sleep(1);
					//Thread.sleep(1);
				} else {
					// wakeup invoked or interrupted
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sc != null) {
				try {
					sc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (!mStoped) { // 异常退出，回调上层
				if (mHandler != null) {
					mHandler.sendEmptyMessage(WHAT_THREAD_END_UNEXCEPTION);
				}
			}
		}
	}

	private int doWrite(SocketChannel channel) throws IOException, InterruptedException {
		ByteBuffer buffer = mWriteQueue.peek();
		if (buffer == null) {
			return 0;
		}
		Assert.assertTrue(buffer.hasRemaining());
		int result = channel.write(buffer);
		if (!buffer.hasRemaining()) {
			mWriteQueue.poll();
			recycle(buffer);
		}
		return result;
	}

	private void recycle(ByteBuffer buffer) throws InterruptedException {
//		Log.i(Thread.currentThread().getName(), "recycle,cache size is " + mCacheQueue.size());
		mCacheQueue.put(buffer);
	}

	private int doRead(SocketChannel channel) throws IOException, InterruptedException {
		ByteBuffer buffer = available(1024);
		buffer.clear();
		int result = 0;
		do { // 
			if (!buffer.hasRemaining()) {
				buffer.clear();
				ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
				newBuffer.put(buffer);
				buffer = newBuffer;
			}
			result = channel.read(buffer);
		} while (!buffer.hasRemaining() && result >= 0);
		if (result > 0) {
			if (mReadQueue.remainingCapacity() < READ_QUEUE_CAPACITY / 2) {
				ByteBuffer bufferDiscard = mReadQueue.poll();
				bufferDiscard.clear();
				recycle(bufferDiscard);
			}
			buffer.flip();
			mReadQueue.offer(buffer);
		} else {
			buffer.clear();
			recycle(buffer);
		}
		if (result > 0) {

		}
		return result;
	}

	public ByteBuffer available(int requestCapacity) {
//		Log.i(Thread.currentThread().getName(), "available,cache size is " + mCacheQueue.size());
		ByteBuffer buffer = mCacheQueue.poll();
		if (buffer == null || buffer.capacity() < requestCapacity) {
			buffer = ByteBuffer.allocate(requestCapacity);
		}
		return buffer;
	}

	@Override
	public void start() {
		mStoped = false;
		start(null);
	}

	@Override
	public void destroy() {
		mStoped = true;
		interrupt();
		try {
			join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void commitWrite(ByteBuffer buffer) throws InterruptedException {
		Assert.assertTrue(buffer.hasRemaining());
		mWriteQueue.put(buffer);
//		Log.i(Thread.currentThread().getName(), "commitWrite,write size is " + mWriteQueue.size());
	}

	public void start(Handler handler) {
		mHandler = handler;
		super.start();
	}
}
