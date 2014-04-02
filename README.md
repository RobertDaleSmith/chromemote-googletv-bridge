Anymote Bridge for Chromemote


Download Chromemote at Chromemote.com

The AnyMote bridge enables Chromemote to connect to Google TVs from ChromeOS devices and also opens up expanded features for all other platforms.

As Google ends support for NPAPI, Chromemote on all platforms will have to switch over to using this bridge.

The Chromemote Anymote Bridge is a HTTP web server running as a background service within an Android app for Google TV. The HTTP server works as a bridge between Chromemote and the AnyMote Java API which is running in the background service as well.

Normally the Anymote client api stuff would all be running in the backend of Chromemote, but with the end of NPAPI support this may be the only path to allowing a Google Chrome Extension to send Anymote commands to a Google TV device.