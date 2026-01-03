| Event name                     | Priority order |
| ------------------------------ | -------------- |
| CommandExecuteEvent            | FIRST          |
| PlayerAvailableCommandsEvent   | FIRST          |
| PlayerChatEvent                | FIRST          |
| PlayerChooseInitialServerEvent | NORMAL         |
| DisconnectEvent                | NORMAL         |
| PlayerChooseInitialServerEvent | FIRST          |
| ServerPreConnectEvent          | FIRST          |
| PlayerChooseInitialServerEvent | NORMAL         |
| GameProfileRequestEvent        | NORMAL         |
| PostLoginEvent                 | FIRST          |
| PreLoginEvent                  | LAST           |