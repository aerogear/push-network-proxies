package org.jboss.aerogear.proxy;

import org.jboss.aerogear.proxy.command.ApnsProxyCommand;
import org.jboss.aerogear.proxy.command.FCMProxyCommand;

import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Help;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
public class ProxyCLI {

    public static void main(String[] args) {
        CliBuilder<Runnable> builder = Cli.<Runnable> builder("proxy")
            .withDefaultCommand(Help.class)
            .withCommand(Help.class)
            .withCommand(ApnsProxyCommand.class)
            .withCommand(FCMProxyCommand.class);

        builder.build().parse(args).run();
    }

}
