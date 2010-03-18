/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.ase.facade;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.provider.Settings.SettingNotFoundException;

import com.google.ase.jsonrpc.Rpc;
import com.google.ase.jsonrpc.RpcDefaultBoolean;
import com.google.ase.jsonrpc.RpcOptionalObject;
import com.google.ase.jsonrpc.RpcParameter;
import com.google.ase.jsonrpc.RpcReceiver;

/**
 * Exposes phone settings functionality.
 * 
 * @author Frank Spychalski (frank.spychalski@gmail.com)
 */
public class SettingsFacade implements RpcReceiver {

  public static int AIRPLANE_MODE_OFF = 0;
  public static int AIRPLANE_MODE_ON = 1;
  
  private final Service mService;
  private final AudioManager mAudio;
  private final WifiManager mWifi;

  /**
   * Creates a new SettingsFacade.
   *
   * @param service
   *          is the {@link Context} the APIs will run under
   */
  public SettingsFacade(Service service) {
    mService = service;
    mWifi = (WifiManager) mService.getSystemService(Context.WIFI_SERVICE);
    mAudio = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
  }

  @Rpc(description = "Set the screen timeout to this number of seconds.", returns = "The original screen timeout.")
  public Integer setScreenTimeout(@RpcParameter("value") Integer value) {
    Integer old_value = getScreenTimeout();
    android.provider.Settings.System.putInt(mService.getContentResolver(),
        android.provider.Settings.System.SCREEN_OFF_TIMEOUT, value * 1000);
    return old_value;
  }

  @Rpc(description = "Returns the current screen timeout in seconds.")
  public Integer getScreenTimeout() {
    try {
      return android.provider.Settings.System.getInt(mService.getContentResolver(),
          android.provider.Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
    } catch (SettingNotFoundException e) {
      return 0;
    }
  }

  @Rpc(description = "Is airplane mode turned on?")
  public Boolean isInAirplaneMode() {
    try {
      return android.provider.Settings.System.getInt(mService.getContentResolver(),
          android.provider.Settings.System.AIRPLANE_MODE_ON) == AIRPLANE_MODE_ON;
    } catch (SettingNotFoundException e) {
      return false;
    }
  }
  
  @Rpc(description = "Toggle Airplane mode. Without argument it will change the current state. " +
                     "Always returns the new value.")
  public Boolean toggleAirplaneMode(
      @RpcOptionalObject("new_airplane_mode") Boolean airplane_mode) {
    boolean set_airplane_mode = airplane_mode == null ? !isInAirplaneMode() : airplane_mode.booleanValue();
    android.provider.Settings.System.putInt(mService.getContentResolver(),
        android.provider.Settings.System.AIRPLANE_MODE_ON, set_airplane_mode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF);
    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
    intent.putExtra("state", set_airplane_mode); 
    mService.sendBroadcast(intent);
    return set_airplane_mode;
  }
  
  @Rpc(description = "Returns the current ringer volume.", returns = "The current volume as an integer.")
  public int getRingerVolume() {
    return mAudio.getStreamVolume(AudioManager.STREAM_RING);
  }

  @Rpc(description = "Sets whether or not the ringer should be silent.")
  public void setRingerSilent(
      @RpcDefaultBoolean(description = "Boolean silent", defaultValue = true) Boolean enabled) {
    if (enabled) {
      mAudio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    } else {
      mAudio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }
  }

  @Rpc(description = "Sets the ringer volume.")
  public void setRingerVolume(@RpcParameter("volume") Integer volume) {
    mAudio.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
  }

  @Rpc(description = "Enables or disables Wifi according to the supplied boolean.")
  public void setWifiEnabled(
      @RpcDefaultBoolean(description = "enabled", defaultValue = true) Boolean enabled) {
    mWifi.setWifiEnabled(enabled);
  }

  @Override
  public void shutdown() {
    // Nothing to do yet.
  }
}
