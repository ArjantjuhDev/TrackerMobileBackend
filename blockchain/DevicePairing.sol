// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract DevicePairing {
    struct Device {
        address owner;
        bool paired;
    }

    mapping(string => Device) public devices; // code => Device

    event DeviceRegistered(string code, address owner);
    event DevicePaired(string code, address owner);

    function registerDevice(string memory code) public {
        require(devices[code].owner == address(0), "Device already registered");
        devices[code] = Device(msg.sender, false);
        emit DeviceRegistered(code, msg.sender);
    }

    function pairDevice(string memory code) public {
        require(devices[code].owner != address(0), "Device not registered");
        require(!devices[code].paired, "Device already paired");
        devices[code].paired = true;
        emit DevicePaired(code, devices[code].owner);
    }

    function isPaired(string memory code) public view returns (bool) {
        return devices[code].paired;
    }
}
