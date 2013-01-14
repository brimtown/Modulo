/*
This file is part of Modulo.

    Modulo is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Modulo is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Modulo.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2012-2013 Tim Brown
*/

package com.modulo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.widget.*;

public class ToneGen extends Activity
{
	private EditText textHz, textVol;
	private Oscillator osc;
	private ToggleButton buttonToggle;
	private SeekBar seekFreq, seekVol;
	private Spinner spinWave;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		osc = new Oscillator();
		
		//View assignments and initial setup
		textHz = (EditText)findViewById(R.id.text_hz);
		textVol = (EditText)findViewById(R.id.text_vol);
		
		buttonToggle = (ToggleButton)findViewById(R.id.toggle_play);
		
		seekFreq = (SeekBar)findViewById(R.id.slider_freq);
		//seekFreq starts at Oscillator.MIN_FREQUENCY, so subtract that from its max value
		seekFreq.setMax(Oscillator.MAX_FREQUENCY - Oscillator.MIN_FREQUENCY);
		//Sets the initial seekFreq progress
		seekFreq.setProgress(osc.getFreq() - Oscillator.MIN_FREQUENCY);
		
		seekVol = (SeekBar)findViewById(R.id.slider_vol);
		seekVol.setMax(100);
		seekVol.setProgress(50);
		
		spinWave = (Spinner)findViewById(R.id.spinner_wave);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.wave_choices, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinWave.setAdapter(adapter);
	}


	@Override
	protected void onStart() {
		super.onStart();
		
		//Updates the frequency once textHz loses focus
		textHz.setOnFocusChangeListener(new View.OnFocusChangeListener() 
		{          
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
				{
					int freqValue = Integer.parseInt(((EditText)v).getText().toString());
					//Prevents a value greater than Oscillator.MAX_FREQUENCY from being sent
					if(freqValue > Oscillator.MAX_FREQUENCY)
					{
						osc.setFreq(Oscillator.MAX_FREQUENCY);
					}
					else
					{
						osc.setFreq(freqValue);
					}
					
					//Since the SeekBar starts at Oscillator.MIN_FREQUENCY, it needs to be subtracted
					//from the value (i.e. 20Hz gets set at 0 on seekFreq)
					seekFreq.setProgress(osc.getFreq() - Oscillator.MIN_FREQUENCY);
				}
			}
		});
		
		/*Updates the volume from textVol once 'enter' has been pressed on the soft keyboard.
		  Since textVol is the final editable view in the activity, the user will be pressing
		  'enter' instead of 'next' like in the previous EditText. Therefore, it makes more sense
		  to wait for 'enter' instead of a loss of focus.*/
		textVol.setOnKeyListener(new View.OnKeyListener() 
		{          
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if(keyCode == KeyEvent.KEYCODE_ENTER)
				{
					//Prevents a value greater than 100 from being sent to the volume
					int volValue = Integer.parseInt(((EditText)v).getText().toString());
					if(volValue > 100)
					{
						osc.setVolume(100);
					}
					else
					{
						osc.setVolume(volValue);
					}
					seekVol.setProgress(osc.getVolume());
					return true;
				}
				else
				{
					return false;
				}
			}
		});

		//Handles the pausing and playing of the oscillator from the ToggleButton
		buttonToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{	
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if(buttonView.isChecked())
				{
					osc.play();
					new Thread(new Runnable()
					{
						public void run()
						{
							//Continuously fills the buffer while the oscillator is playing
							while(osc.getIsPlaying())
							{
								osc.fillBuffer();
							}
						}
					}).start();			
				}
				else
				{
					osc.pause();
				}
			}
		});

		//Updates the oscillator's frequency based on the progress of seekFreq
		seekFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				osc.setFreq(Oscillator.MIN_FREQUENCY + seekBar.getProgress());
				textHz.setText("" + osc.getFreq());
			}
		});

		//Updates the oscillator's volume based on the progress of seekVol
		seekVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				osc.setVolume(seekBar.getProgress());
				textVol.setText("" + osc.getVolume());
			}
		});

		//Updates the oscillator's waveform based on the selection in spinWave
		spinWave.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
			{
				osc.setWave(pos);
			}
			public void onNothingSelected(AdapterView<?> parent)
			{
			}
		});
	}

	//Stops the oscillator playback and releases the AudioTrack resources 
	@Override
	protected void onPause() {
		super.onPause();
		osc.stop();
		buttonToggle.setChecked(false);
	}
}
