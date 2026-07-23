# AjQueue Integration

NavAuth provides a separate plugin that integrates with [AjQueue](https://www.spigotmc.org/resources/ajqueue.99086/), a popular queue management system for Velocity proxy.

## How it Works

The integration plugin listens for NavAuth's `UserAuthenticatedEvent`. When a player successfully authenticates, the plugin automatically adds them to a configurable AjQueue queue.

Additionally, it can optionally cancel AjQueue's `PreQueueEvent` for players who are **not yet authenticated**, preventing unauthenticated players from joining a server through the queue.

## Features

- Automatically adds authenticated players to a specified AjQueue queue
- Optionally block queue join for unauthenticated players
- Configurable failure handling (message + optional kick)

## Download

The integration plugin jar is available on the [GitHub releases page](https://github.com/Navio1430/NavAuth).

## Installation

1. Place the `navauth-ajqueue-integration-XXX.jar` in your Velocity server's `plugins/` directory
2. Ensure both **NavAuth** and **AjQueue** are installed
3. Restart your proxy
