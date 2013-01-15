package com.modulo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Oscillator
{
	private int frequency;
	public static final int MAX_FREQUENCY = 20000;
	public static final int MIN_FREQUENCY = 20;
	private boolean isPlaying;
	private AudioTrack audioTrack;
	private int volume;
	private final int sampleRate = 44100;
	private int bufferSize;
	private short[] sample, buffer, sin, sqr, tri, saw;
	private int phase;

	public Oscillator()
	{
		//Sets the initial frequency to 440Hz, or "concert A"
		frequency = 440;
		volume = 50;
		isPlaying = false;
		bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

		//Initializes an array of shorts for the buffer and the currently loaded waveform
		buffer = new short[bufferSize];
		sample = new short[sampleRate];

		audioTrack = initializeAudioTrack();

		//Initializes an array of shorts for each individual waveform
		sqr = new short[sampleRate];
		sin = new short[sampleRate];
		tri = new short[sampleRate];
		saw = new short[sampleRate];

		//Pre-generates the lookup tables for each waveform, allowing the user to switch
		//between waveforms without significant delay
		sqr = generateSqr();
		sin = generateSin();
		tri = generateTri();
		saw = generateSaw();

		//Sets sine as the initial waveform
		sample = sin;
	}

	public void pause()
	{
		//Checks if audioTrack is initialized and if so, immediately pauses and flushes the audio data
		if(audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
		{
			setIsPlaying(false);
			audioTrack.pause();
			audioTrack.flush();
		}
	}

	//Sets up an AudioTrack
	public AudioTrack initializeAudioTrack()
	{
		AudioTrack returnTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, buffer.length,
				AudioTrack.MODE_STREAM);

		returnTrack.setStereoVolume(AudioTrack.getMaxVolume()*(float)(volume/100.0f), AudioTrack.getMaxVolume()*(float)(volume/100.0f));

		return returnTrack;
	}

	public void play()
	{
		if(audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
		{
			setIsPlaying(true);
			fillBuffer();
			audioTrack.play();
		}
		else
		{
			audioTrack = initializeAudioTrack();
			setIsPlaying(true);
			fillBuffer();
			audioTrack.play();
		}
	}

	public void stop()
	{
		if(audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
		{
			setIsPlaying(false);
			audioTrack.pause();
			audioTrack.flush();
			audioTrack.release();
		}
	}
	public void setFreq(int freq)
	{
		frequency = freq;
	}
	public int getFreq()
	{
		return frequency;
	}
	public void setIsPlaying(boolean boo)
	{
		isPlaying = boo;
	}
	public boolean getIsPlaying()
	{
		return isPlaying;
	}
	public void setVolume(int vol)
	{
		volume = vol;
		audioTrack.setStereoVolume(AudioTrack.getMaxVolume()*(float)(volume/100.0f), AudioTrack.getMaxVolume()*(float)(volume/100.0f));
	}
	public int getVolume()
	{
		return volume;
	}
	
	//Generates the sine wave lookup table
	private short[] generateSin()
	{
		short[] returnArray = new short[sampleRate];
		for(int i=0; i<sampleRate; i++)
		{
			returnArray[i]=(short)((float) Short.MIN_VALUE * (Math.sin((2.0f*(float)Math.PI*((float)i))/((float)sampleRate))));
		}
		return returnArray;
	}
	
	//Generates the sawtooth wave lookup table
	private short[] generateSaw()
	{
		short[] returnArray = new short[sampleRate];
		for(int i=0; i<sampleRate; i++)
		{
			returnArray[i]=(short) ((float)Short.MIN_VALUE + (((float)i *(2.0f * (float)Short.MAX_VALUE))/(float) sampleRate));	
		}
		return returnArray;
	}
	
	//Generates the triangle wave lookup table
	private short[] generateTri()
	{
		short[] returnArray = new short[sampleRate];
		
		//Since a triangle wave is a piecewise function, it requires several 'if' statements
		for(int i=0; i<sampleRate; i++)
		{
			//First quarter of the waveform lookup table
			if(i<(sampleRate/4))
			{
				returnArray[i]=(short)(((float)i*(4.0f * (float)Short.MAX_VALUE))/(float) sampleRate);
			}
			
			//Last quarter of the waveform lookup table
			else if(i>(3* (sampleRate/4)))
			{
				returnArray[i]=(short)((((float)i *(4.0f * (float)Short.MAX_VALUE))/(float) sampleRate)-2.0f*Short.MAX_VALUE);
			}
			
			//Middle two sections of the waveform lookup table
			else
			{
				returnArray[i]= (short) ((float)2.0f*Short.MAX_VALUE-(((float)i *(4.0f * (float)Short.MAX_VALUE))/(float) sampleRate));
			}
		}
		return returnArray;
	}
	
	//Generates the square wave lookup table
	private short[] generateSqr()
	{
		short[] returnArray = new short[sampleRate];
		
		//The square wave is represented by two values (Short.MAX_VALUE and Short.MIN_VALUE)
		//with the max value for the first half of the waveform, and the min for the second
		for(int i=0; i<sampleRate; i++)
		{
			if(i<(sampleRate/2))
			{
				returnArray[i]=Short.MAX_VALUE;
			}
			else
			{
				returnArray[i]=Short.MIN_VALUE;
			}
		}
		return returnArray;
	}

	public void fillBuffer()
	{
		//For each location in the buffer
		for(int i=0; i<buffer.length; i++)
		{
			//Causes phase to wrap around to the front of the lookup table
			//when it exceeds its length
			phase%=sample.length;
			
			//Sets the short at buffer[i] to the value found in the lookup table
			//of the current waveform sample
			buffer[i]=sample[phase];
			
			//Increments the phase by the current frequency, allowing it to step
			//through the table at the correct speed and resulting in a change of pitch
			phase += frequency;
		}
		
		//Writes the buffer to the audioTrack
		audioTrack.write(buffer, 0, buffer.length);
	}

	//Changes the waveform
	public void setWave(int pos)
	{
		switch(pos)
		{
		case 0: sample = sin;
		break;
		case 1: sample = saw;
		break;
		case 2: sample = sqr;
		break;
		case 3: sample = tri;
		break;
		default: sample = sin;
		break;
		}
	}
}
