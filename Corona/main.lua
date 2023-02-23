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



