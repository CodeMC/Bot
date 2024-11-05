[discord]: https://discord.gg/AGcFMu6
[codemc]: https://ci.codemc.io
[docs]: https://docs.codemc.io
[license]: https://github.com/CodeMC/Bot/blob/master/LICENSE
[api]: https://github.com/CodeMC/API

# CodeMC Bot
This is the code of the CodeMC Discord bot used on the [Discord Server][discord] of [CodeMC.io][codemc].

## Purpose
The main purpose of the bot is to handle join-requests from users, which want to use the public CI of the website.

## How it Works
You simply have to type `/submit` to submit a join request. You will be prompted with a few questions which you have to answer.

After some validation checks will the bot ask you in DMs if you actually want to submit the provided info, which you can confirm or deny.  
When confirming will the bot post an entry in the `#request-access` channel of the server which a mod or admin can then review and either accept or deny.

The bot uses the [CodeMC API][api] to process, store, and implement the join requests. Depending on whether you are accepted or denied,
the bot will create you a new Jenkins and Nexus account, or it will send you a message with the reason why you were denied.

## License
This project is licensed under MIT. Please read the [LICENSE file][license] for more information.

## Links

- [CodeMC Website (CI)][codemc]
- [CodeMC API][api]
- [Documentation][docs]
- [Discord Server][discord]
