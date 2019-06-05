// IUpgradeSendInterface.aidl
package com.transsion.appupgrade;

import com.transsion.appupgrade.IUpgradeRequestInterface;

// Declare any non-default types here with import statements

interface IUpgradeSendInterface {

    void registerCallback(IUpgradeRequestInterface callback);

    void unregisterCallback(IUpgradeRequestInterface callback);

    String sendCommand(String command);
}
