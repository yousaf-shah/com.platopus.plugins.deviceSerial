//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.deviceSerial;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.ansca.corona.permissions.PermissionState;
import com.ansca.corona.permissions.PermissionsServices;
import com.ansca.corona.permissions.PermissionsSettings;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;

import android.app.Service;

import android.content.ComponentName;
import android.content.Context;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/** Lua registry ID to the Lua function to be called when the ad request finishes. */
	private int fListener;
	private Handler handler = new Handler(Looper.getMainLooper());
	private static final String DATA = "data";
	private Context activity;

	private static final String SOURCE = "source_byte";


	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	@SuppressWarnings("unused")
	public LuaLoader() {
		// Initialize member variables.
		fListener = CoronaLua.REFNIL;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called every time a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
				new getDeviceProperty(),
				new getDeviceSerialProperties(),
				new getDeviceSerial(),
				new getPhoneState(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.

	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
	}

	/**
	 * Simple example on how to dispatch events to Lua. Note that events are dispatched with
	 * Runtime dispatcher. It ensures that Lua is accessed on it's thread to avoid race conditions
	 * @param message simple string to sent to Lua in 'message' field.
	 */
//	@SuppressWarnings("unused")
	public class getDeviceProperty implements com.naef.jnlua.NamedJavaFunction {
		// This reports a class name back to Lua during the initiation phase.
		@Override
		public String getName() {
			return "getDeviceProperty";
		}

		// This is what actually gets invoked by the Lua call
		@Override
		public int invoke(final LuaState luaState) {

			String deviceProperty = "";
			String propertyName = "";
			Boolean serialNumberFound = false;

			// check number or args
			int nargs = luaState.getTop();
			if ((nargs < 1) || (nargs > 1)){
				Log.e("getDeviceProperty", "Expected 1 argument, got " + nargs);
				return 0;
			}

			// get site name
			if (luaState.type(1) == LuaType.STRING) {
				propertyName = luaState.toString(1);
			} else {
				Log.e("getDeviceProperty", "propertyName (string) expected, got " + luaState.typeName(1));
				return 0;
			}

			try {
				deviceProperty = getDeviceProperty(propertyName);

				if (deviceProperty == null) {
					serialNumberFound = false;
					deviceProperty = "";
					Log.e("getDeviceProperty", "no property returned");
				} else {
					serialNumberFound = true;
//					Log.e("getDeviceProperty", "Property, value = " + deviceProperty);
				}
				luaState.pushString(deviceProperty);

			} catch (Exception e) {
//				e.printStackTrace();
				luaState.pushString("");
			}

			return 1;
		}

	}
	public class getDeviceSerialProperties implements com.naef.jnlua.NamedJavaFunction {
		// This reports a class name back to Lua during the initiation phase.
		@Override
		public String getName() {
			return "getDeviceSerialProperties";
		}

		// This is what actually gets invoked by the Lua call
		@Override
		public int invoke(final LuaState luaState) {

			String serialNumberProperties = "";
			Boolean serialNumberFound = false;

			// check number or args
			int nargs = luaState.getTop();
			if (nargs > 0 ){
				Log.e("getDeviceSerialProperties","no arguments expected, got: " + nargs);
				return 0;
			}

			try {
				serialNumberProperties = getSerialNumberProperties();

				if (serialNumberProperties == null) {
					serialNumberFound = false;
					serialNumberProperties = "";
					Log.e("getDeviceSerialProperties", "no serial number returned");
				} else {
					serialNumberFound = true;
//					Log.e("getDeviceSerialProperties", "Serial Number, value = " + serialNumberProperties);
				}
				luaState.pushString(serialNumberProperties);

			} catch (Exception e) {
//				e.printStackTrace();
				luaState.pushString("");
			}

			return 1;
		}

	}

	public class getDeviceSerial implements com.naef.jnlua.NamedJavaFunction {
		// This reports a class name back to Lua during the initiation phase.
		@Override
		public String getName() {
			return "getDeviceSerial";
		}

		// This is what actually gets invoked by the Lua call
		@Override
		public int invoke(final LuaState luaState) {

			String serialNumber = "";
			Boolean serialNumberFound = false;

			// check number or args
			int nargs = luaState.getTop();
			if (nargs > 0 ){
				Log.e("getDeviceSerial","no arguments expected, got: " + nargs);
				return 0;
			}

			try {
				serialNumber = getSerialNumber();

				if (serialNumber == null) {
					serialNumberFound = false;
					serialNumber = "";
					Log.e("getDeviceSerial", "no serial number returned");
				} else {
					serialNumberFound = true;
//					Log.e("getDeviceSerial", "Serial Number, value = " + serialNumber);
				}
				luaState.pushString(serialNumber);

			} catch (Exception e) {
//				e.printStackTrace();
				luaState.pushString("");
			}

			return 1;
		}

	}
	public class getPhoneState implements com.naef.jnlua.NamedJavaFunction {
		// This reports a class name back to Lua during the initiation phase.
		@Override
		public String getName() {
			return "getPhoneState";
		}

		// This is what actually gets invoked by the Lua call
		@Override
		public int invoke(final LuaState luaState) {

			String phoneState = "";
			Boolean phoneStateFound = false;
			Boolean askForPermission = false;

			// check number or args
			int nargs = luaState.getTop();
			if (nargs < 1 || nargs > 1 ){
				Log.e("getPhoneState","1 argument expected, got: " + nargs);
				return 0;
			}
			// get ask flag
			if (luaState.type(1) == LuaType.BOOLEAN) {
				askForPermission = luaState.toBoolean(1);
			} else {
				Log.e("getDeviceSerial", "askForPermission (boolean) expected, got: " + luaState.typeName(1));
				return 0;
			}

			try {
				phoneStatePermissionsResultHandler resultHandler = new phoneStatePermissionsResultHandler();
				phoneState = resultHandler.handleGetPhoneState(askForPermission);

				if (phoneState == null) {
					phoneStateFound = false;
					phoneState = "";
					Log.e("getDeviceSerial", "no phone state returned");
				} else {
					phoneStateFound = true;
//					Log.e("getDeviceSerial", "Phone State, value = " + phoneState);
				}
				luaState.pushString(phoneState);

			} catch (Exception e) {
//				e.printStackTrace();
				luaState.pushString("ERROR");
			}

			return 1;
		}

	}

	public String getSerialNumber() {
		String serialNumber = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL;
		return TextUtils.isEmpty(serialNumber) ? "" : serialNumber + "";
	}
	
	public String getSerialNumberProperties() {

		String serialNumber = "";

		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);

			serialNumber = (String) get.invoke(c, "ro.serialno");
			if (serialNumber.equals(""))
				serialNumber = (String) get.invoke(c, "ro.boot.serialno");
			if (serialNumber.equals(""))
				serialNumber = (String) get.invoke(c, "persist.sys.hwblk.sn");
			if (serialNumber.equals(""))
				serialNumber = "";
		} catch (Exception e) {
			e.printStackTrace();
			serialNumber = "";
		}

		return TextUtils.isEmpty(serialNumber) ? "" : serialNumber + "";
	}

	public String getDeviceProperty(String propertyName) {

		String serialNumber = "";

		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);

			serialNumber = (String) get.invoke(c, propertyName);
			if (serialNumber.equals(""))
				serialNumber = "";
		} catch (Exception e) {
			e.printStackTrace();
			serialNumber = "";
		}

		return TextUtils.isEmpty(serialNumber) ? "" : serialNumber + "";
	}

	private class phoneStatePermissionsResultHandler implements CoronaActivity.OnRequestPermissionsResultHandler {
		public String handleGetPhoneState(Boolean askForPermission) {
			// Check for WRITE_EXTERNAL_STORAGE permission
			PermissionsServices permissionsServices = new PermissionsServices(CoronaEnvironment.getApplicationContext());
			PermissionState phonePermissionState = permissionsServices.getPermissionStateFor(PermissionsServices.Permission.READ_PHONE_STATE);
			switch (phonePermissionState) {
				case MISSING:
					// The Corona developer didn't add the permission to the AndroidManifest.xml
					// As it is required for our app to function, we'll error out here
					// If the permission were not critical, we could work around it here
					permissionsServices.showPermissionMissingFromManifestAlert(PermissionsServices.Permission.READ_PHONE_STATE, "Platopus needs access to this device's Serial Number");
					return "MISSING" ;
				case DENIED:
					if (!permissionsServices.shouldNeverAskAgain(PermissionsServices.Permission.READ_PHONE_STATE)) {
						// Create our Permissions Settings to compare against in the handler
						if(askForPermission) {
							PermissionsSettings settings = new PermissionsSettings(PermissionsServices.Permission.READ_PHONE_STATE);
							// Request Write External Storage permission
							permissionsServices.requestPermissions(settings, this);
							return "REQUESTED";
						} else {
							return "REQUEST";
						}
					} else {
						return "NEVER" ;
					}
				default:
					// Permission is granted!
					return "OK" ;
			}
		}

		@Override
		public void onHandleRequestPermissionsResult(CoronaActivity activity, int requestCode, String[] permissions, int[] grantResults) {
			// Clean up and unregister our request (you should always do this)
			PermissionsSettings permissionsSettings = activity.unregisterRequestPermissionsResultHandler(this);
			if (permissionsSettings != null) {
				permissionsSettings.markAsServiced();
			}


			Log.e("phoneState", "Event Received");

			activity.getRuntimeTaskDispatcher().send( new CoronaRuntimeTask() {
				@Override
				public void executeUsing(CoronaRuntime runtime) {
					LuaState L = runtime.getLuaState();

					CoronaLua.newEvent( L, "phoneStateDialog" );

					L.pushString(permissions[0]);
					L.setField(-2, "grantPermission");

					if(grantResults[0] == -1) {
						L.pushString("DENIED");
					} else if(grantResults[0] == 0) {
						L.pushString("GRANTED");
					} else {
						L.pushString(String.valueOf(grantResults[0]));
					}
					L.setField(-2, "grantResult");


					try {
						Log.e("phoneState", "Dispatch Event ...");
						CoronaLua.dispatchRuntimeEvent( L, 0 );
					} catch (Exception ignored) {
						Log.e("phoneState", "Could Not Dispatch Event");
					}
				}
			} );

		}
	}
}


