package com.mail.smtp.dns;

import com.mail.smtp.config.SmtpConfig;
import com.mail.smtp.dns.handler.DnsResponseHandlerA;
import com.mail.smtp.dns.handler.DnsResponseHandlerMX;
import com.mail.smtp.dns.handler.DnsResponseHandlerTXT;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@Configuration
public class DnsConfiguration
{
    @Autowired
    private SmtpConfig smtpConfig;

    @Bean
    @Lazy
    public Bootstrap tcpMxBootstrap()
    {
        int dnsTimeout = smtpConfig.getInt("smtp.dns.timeout", 10);
        Bootstrap b = new Bootstrap();
        //b.group(tcpMxEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, dnsTimeout * 1000);
        b.handler(new ChannelInitializer< SocketChannel >()
        {
            @Override
            protected void initChannel(SocketChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                //tcp protocol
                p.addLast(new ReadTimeoutHandler(dnsTimeout))
                    .addLast(new WriteTimeoutHandler(dnsTimeout))
                    .addLast(new TcpDnsQueryEncoder())
                    .addLast(new TcpDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerMX<>(DefaultDnsResponse.class));
            }
        });
        return b;
    }

    @Bean
    @Lazy
    public Bootstrap udpMxBootstrap()
    {
        //UDP 의 경우 timeout 처리는 resolver 내에서 처리한다.
        Bootstrap b = new Bootstrap();
        //b.group(udpMxEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioDatagramChannel.class);
        b.handler(new ChannelInitializer< DatagramChannel >()
        {
            @Override
            protected void initChannel(DatagramChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                p.addLast(new DatagramDnsQueryEncoder())
                    .addLast(new DatagramDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerMX<>(DatagramDnsResponse.class));
            }
        });
        return b;
    }

    @Bean
    @Lazy
    public Bootstrap tcpTxtBootstrap()
    {
        int dnsTimeout = smtpConfig.getInt("smtp.dns.timeout", 10);
        Bootstrap b = new Bootstrap();
        //b.group(tcpTxtEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, dnsTimeout * 1000);
        b.handler(new ChannelInitializer< SocketChannel >()
        {
            @Override
            protected void initChannel(SocketChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                p.addLast(new ReadTimeoutHandler(dnsTimeout))
                    .addLast(new WriteTimeoutHandler(dnsTimeout))
                    .addLast(new TcpDnsQueryEncoder())
                    .addLast(new TcpDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerTXT<>(DefaultDnsResponse.class));
            }
        });
        return b;
    }

    @Bean
    @Lazy
    public Bootstrap udpTxtBootstrap()
    {
        //UDP 의 경우 timeout 처리는 resolver 내에서 처리한다.
        Bootstrap b = new Bootstrap();
        //b.group(udpTxtEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioDatagramChannel.class);
        b.handler(new ChannelInitializer< DatagramChannel >()
        {
            @Override
            protected void initChannel(DatagramChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                p.addLast(new DatagramDnsQueryEncoder())
                    .addLast(new DatagramDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerTXT<>(DatagramDnsResponse.class));
            }
        });
        return b;
    }

    @Bean
    @Lazy
    public Bootstrap tcpABootstrap()
    {
        int dnsTimeout = smtpConfig.getInt("smtp.dns.timeout", 10);
        Bootstrap b = new Bootstrap();
        //b.group(tcpAEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, dnsTimeout * 1000);
        b.handler(new ChannelInitializer< SocketChannel >()
        {
            @Override
            protected void initChannel(SocketChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                p.addLast(new ReadTimeoutHandler(dnsTimeout))
                    .addLast(new WriteTimeoutHandler(dnsTimeout))
                    .addLast(new TcpDnsQueryEncoder())
                    .addLast(new TcpDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerA<>(DefaultDnsResponse.class));
            }
        });
        return b;
    }

    @Bean
    @Lazy
    public Bootstrap udpABootstrap()
    {
        //UDP 의 경우 timeout 처리는 resolver 내에서 처리한다.
        Bootstrap b = new Bootstrap();
        //b.group(udpAEventLoopGroup());
        b.group(eventLoopGroup());
        b.channel(NioDatagramChannel.class);
        b.handler(new ChannelInitializer< DatagramChannel >()
        {
            @Override
            protected void initChannel(DatagramChannel socketChannel)
            {
                ChannelPipeline p = socketChannel.pipeline();

                p.addLast(new DatagramDnsQueryEncoder())
                    .addLast(new DatagramDnsResponseDecoder())
                    .addLast(new DnsResponseHandlerA<>(DatagramDnsResponse.class));
            }
        });
        return b;
    }

    @Bean(destroyMethod = "shutdownGracefully")
    @Lazy
    public EventLoopGroup eventLoopGroup()
    {
        return new NioEventLoopGroup();
    }

    @Bean
    public Executor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor tp = new ThreadPoolTaskExecutor();
        tp.setCorePoolSize(10);
        tp.setQueueCapacity(50);
        tp.setMaxPoolSize(100);
        tp.setThreadNamePrefix("dnsResolve");
        tp.setTaskDecorator(new TaskDecorator()
        {
            @Override
            public Runnable decorate(Runnable runnable)
            {
                Map<String, String> mdcMap = MDC.getCopyOfContextMap();
                if( mdcMap != null )
                {
                    return () -> {
                        MDC.setContextMap(mdcMap);
                        runnable.run();
                    };
                }
                else
                    return () -> runnable.run();
            }
        });
        tp.initialize();
        return tp;
    }
}
