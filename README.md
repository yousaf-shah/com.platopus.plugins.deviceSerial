# deviceSerial-Plugin

Solar2D plugin to get the serial number from Android devices which support this functionality.

To get the device serial:

String serialNumber = deviceSerial.getDeviceSerial()

This returns the device's serial number. This will return an empty string ("") if the device does not support this functionality, or you are running an Android version >= 26 and have not specifically requested the PHONE_STATE permission.

It is best to test the permission first, and a function is provided to do this:

String phoneState = deviceSerial.getPhoneState(Boolean askForPermission)

askForPermission:
 - Set this to false to return the PHONE_STATE permission with no interaction.
 - Set this to true to force a dialog asking for permission to use the phone (needed to get the serial number)

The returned phoneState may contain:

- **MISSING** = Your app does not have android.permissions.READ_PHONE_STATE in your build.settings (i.e. in the AndroidManifest.xml)
- **REQUEST** = You need to request the user to grant the PHONE_STATE permission
- **OK** = Permission is granted and the serial number will be returned if you ask for it.
- **NEVER** = The user has already responded to the permissions dialog and ticked the 'Do not ask me again' checkbox.
- **REQUESTED** = in the case that you called getPhoneState with askForPermission set to true, this indicates that the dialog box was presented to the user asking for permission.

 In the case that you ask for permission, an event will be dispatched to the Runtime event handler with the result of the user's interaction with the dialog.
 
 You can receive this event by setting up a listener: 
 
Runtime:addEventListener("phoneStateDialog", yourListenerName)

This event will be triggered with two variables:

event.**grantPermission**
This will always return **android.permissions.READ_PHONE_STATE**

event.**grantResult**
This will return either:
**DENIED** = the user denied the request for phone permissions.
**GRANTED** = the user granted the request for phone permissions - you can request the serial number after receiving this.

```lua
-- Basic example to get the serial number from a device which supports this function.
-- also include phone state functionality to allow you to check and request the require PHONE_STATE permissions

local deviceSerial = require ( "plugin.deviceSerial" )

local function phoneStateListener(event)

     native.showAlert("LISTENER",event.grantResult,{"OK"})

end

Runtime:addEventListener("phoneStateDialog", phoneStateListener)

local phoneState = deviceSerial.getPhoneState(true)

if phoneState == "GRANTED" then

     native.showAlert("SERIAL",deviceSerial.getDeviceSerial(),{"OK"})

else
     -- native.showAlert("SERIAL",phoneState,{"OK"})
end

````


