[discord]: https://discord.gg/AGcFMu6
[codemc]: https://ci.codemc.io
[docs]: https://docs.codemc.io
[license]: https://github.com/CodeMC/Bot/blob/master/LICENSE

# CodeMC Bot
This is the code of the CodeMC Discord bot used on the [Discord Server][discord] of [CodeMC.io][codemc].

## Purpose
The main purpose of the bot is to handle join-requests from users, which want to use the public CI of the website.

## How it works
You simply have to type `/submit <github username or url> <repository url> <message>` to submit a join request.

The first argument can either be a username to your GitHub Profile, or a direct URL to it (recommendet).  
The second argument has to be URL to a repository.  
The third one is a short description of why you want to join. (Due to a Discord limitation can you not use line breaks in your description).

After some validation checks will the bot ask you in DMs if you actually want to submit the provided info, which you can confirm or deny.  
When confirming will the bot post an entry in the `#request-access` channel of the server which a mod or admin can then review and either accept or deny.

## License
This project is licensed under MIT. Please read the [LICENSE file][license] for more information.

## Links

- [CodeMC Website (CI)][codemc]
- [Documentation][docs]
- [Discord Server][discord]
