/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.streaming.rtp;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.util.Log;

/**
 * An InputStream that uses data from a MediaCodec. The purpose of this class is
 * to interface existing RTP packetizers of libstreaming with the new MediaCodec
 * API. This class is not thread safe !
 */
@SuppressLint("NewApi")
public class AudioMediaCodecInputStream extends MediaCodecInputStream {

	private int mProfile;
	private int mSampleRateIdx;
	private int mChannel;

	public AudioMediaCodecInputStream(MediaCodec mediaCodec, int profile, int samplingRateIndex, int channel) {
		super(mediaCodec);
		mProfile = profile;
		mSampleRateIdx = samplingRateIndex;
		mChannel = channel;
	}

	public final String TAG = "MediaCodecInputStream";
	private long mTS = 0;

	@Override
	public void close() {
		super.close();
		mTS = 0l;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int min = 0;
		try {
			if (mBuffer == null) {
				mBuffer = ByteBuffer.allocate(10240);
				while (!Thread.interrupted() && !mClosed) {
					mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 50000);
					if (mIndex >= 0) {
						if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
							mTS = System.nanoTime() / 1000;
							continue;
						} else {
							Log.d(TAG, "mBufferInfo.FLG: " + mBufferInfo.flags + " Time differ : " + (mBufferInfo.presentationTimeUs - mTS) + " size: " + mBufferInfo.size);
							if (mTS == 0l) {
								mTS = mBufferInfo.presentationTimeUs;
							} else {
								mTS += 80000;
							}
						}
						mBuffer.clear();
						mBuffer.position(7);
						// mBuffer.put(mBuffers[mIndex].array(), 0,
						// mBufferInfo.size);

						mBuffers[mIndex].get(mBuffer.array(), 7, mBufferInfo.size);
						mBuffers[mIndex].clear();
						mBuffer.position(mBuffer.position() + mBufferInfo.size);
						addADTStoPacket(mBuffer.array(), mBufferInfo.size + 7);
						mBuffer.flip();
						break;
					} else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						mBuffers = mMediaCodec.getOutputBuffers();
					} else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						mMediaFormat = mMediaCodec.getOutputFormat();
						Log.i(TAG, mMediaFormat.toString());
					} else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						Log.v(TAG, "No buffer available...");
						// return 0;
					} else {
						Log.e(TAG, "Message: " + mIndex);
						// return 0;
					}
				}
			}

			if (mClosed)
				throw new IOException("This InputStream was closed");

			int size = mBufferInfo.size + 7;
			min = length < size - mBuffer.position() ? length : size - mBuffer.position();
			mBuffer.get(buffer, offset, min);

			if (mBuffer.position() >= size) { // read complete
				mMediaCodec.releaseOutputBuffer(mIndex, false);
				mBuffer = null;
			}

		} catch (RuntimeException e) {
			e.printStackTrace();
		}

		return min;
	}

	/**
	 * us
	 * 
	 * @return
	 */
	public long readTs() {
		return mTS;
	}

	private void addADTStoPacket(byte[] packet, int packetLen) {
		// int profile = 2; // AAC LC
		// // 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
		// int freqIdx = 4; // 44.1KHz
		// int chanCfg = 2; // CPE

		// fill in ADTS data
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF1;
		packet[2] = (byte) (((mProfile - 1) << 6) + (mSampleRateIdx << 2) + (mChannel >> 2));
		packet[3] = (byte) (((mChannel & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}
}
