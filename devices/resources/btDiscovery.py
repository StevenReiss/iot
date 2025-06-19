import asyncio
from bleak import BleakScanner


async def run():
    print("Scanning for BLE devices...")
    devices = await BleakScanner.discover()
#   sorted_devices = sorted(devices, key=lambda device: device.rssi, reverse=True)
    sorted_devices = devices
    for device in sorted_devices:
        print(f"Device ({device.address}): {device.name}")


loop = asyncio.get_event_loop()
loop.run_until_complete(run())
