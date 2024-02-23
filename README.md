# sipgate-dnd
Quick-and-dirty Android app for toggling sipgate team DND status of a device. Not recommended for production use, not affiliated with sipgate.

## Setup

- Go to https://app.sipgate.com/personal-access-token
- Create a new token with the following permissions:
  - devices
    - devices:read
    - devices:write
    - devices:forwardings:read
    - devices:forwardings:write
  - groups
    - groups:read
    - groups:devices:write
    - groups:numbers:read
    - groups:users:read
  - phonelines
    - phonelines:busyonbusy:read
    - phonelines:busyonbusy:write
    - phonelines:devices:read
    - phonelines:devices:write
    - phonelines:forwardings:read
    - phonelines:forwardings:write
    - phonelines:read
    - phonelines:write
- Note down username and password
- Find out your account-specific user ID, looks like `w23`. You might need to ask your admin.
- Find out your phone's device ID