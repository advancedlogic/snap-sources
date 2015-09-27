package commands.base

welcome = { ->
    def hostName;
    try {
        hostName = java.net.InetAddress.getLocalHost().getHostName();
    } catch (java.net.UnknownHostException ignore) {
        hostName = "localhost";
    }
    return """

        ///
    (o)(o)  ${crash.context.version}
    (*_*)
    `(_)
    `(_)
    "(_) (_)(_)

Follow and support the project on http://www.crashub.org
Welcome to $hostName + !
It is ${new Date()} now
""";
}

prompt = { ->
    return "snap> ";
}
