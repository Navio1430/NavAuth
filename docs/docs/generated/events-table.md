| Event name                     | Priority order |
| ------------------------------ | -------------- |
| CommandExecuteEvent            | FIRST          |
| PlayerAvailableCommandsEvent   | FIRST          |
| PlayerChatEvent                | FIRST          |
| DisconnectEvent                | NORMAL         |
| PlayerChooseInitialServerEvent | FIRST          |
| ServerPreConnectEvent          | FIRST          |
| PlayerChooseInitialServerEvent | NORMAL         |
| PlayerChooseInitialServerEvent | NORMAL         |
| GameProfileRequestEvent        | NORMAL         |
| PostLoginEvent                 | FIRST          |
| PreLoginEvent                  | LAST           |