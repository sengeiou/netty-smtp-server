/*
auther : ddibkh
description : DNS resolver
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */

package com.mail.smtp.dns;

import com.mail.smtp.config.SmtpConfig;
import com.mail.smtp.dns.handler.DnsResponseHandlerA;
import com.mail.smtp.dns.result.DnsResult;
import com.mail.smtp.exception.DnsException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.dns.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component("dnsResolverA")
@RequiredArgsConstructor
@Lazy
//@EnableAsync
public class DnsResolverA implements DnsResolver
{
    private final Logger log = LoggerFactory.getLogger("delivery");
    private final SmtpConfig smtpConfig;
    private final Bootstrap tcpABootstrap;
    private final Bootstrap udpABootstrap;

    //@Async("threadPoolTaskExecutor")
//    public ListenableFuture<List< DnsResult >> resolveDomainByTcp(String domainName) throws DnsException
    public List< DnsResult > resolveDomainByTcp(String domainName) throws DnsException
    {
        String dnsIp = smtpConfig.getString("smtp.dns.ip", "8.8.8.8");
        Integer dnsTimeout = smtpConfig.getInt("smtp.dns.timeout", 30);

        short randomID = (short)new Random().nextInt(1 << 15);

        final Channel ch;
        try
        {
            ch = tcpABootstrap.connect(dnsIp, 53).sync().channel();
        }
        catch( Throwable cte )
        {
            log.error("fail to connect dns server, {}", cte.getMessage());
            throw new DnsException(
                    String.format("fail to connect dns server, %s", cte.getMessage()));
        }

        DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
                .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, DnsRecordType.A))
                .setRecursionDesired(true);

        try
        {
            ch.writeAndFlush(query).sync().addListener(
                    new GenericFutureListener<>()
                    {
                        @Override
                        public void operationComplete(Future< ? super Void > future)
                        {
                            if( !future.isSuccess() )
                                throw new DnsException("fail send query message");
                            else if( future.isCancelled() )
                                throw new DnsException("operation cancelled");
                        }
                    }
            );

            boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);

            //timeout occured
            if( !bSuccess )
            {
                log.error("fail to resolve domain by TCP, timed out, domain : {}, dns : {}", domainName, dnsIp);
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by TCP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            log.error("fail to resolve A record, interrupted exception");
            throw new DnsException("fail to resolve A record, interrupted exception");
        }

        List< DnsResult > list = ch.pipeline().get(DnsResponseHandlerA.class).getResult();
        /*return new AsyncResult<>(list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList()));*/
        return list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList());
    }

    //@Async("threadPoolTaskExecutor")
    //public ListenableFuture< List<DnsResult> > resolveDomainByUdp(String domainName) throws DnsException
    public List<DnsResult> resolveDomainByUdp(String domainName) throws DnsException
    {
        String dnsIp = smtpConfig.getString("smtp.dns.ip", "8.8.8.8");
        Integer dnsTimeout = smtpConfig.getInt("smtp.dns.timeout", 10);

        short randomID = (short)new Random().nextInt(1 << 15);

        InetSocketAddress addr = new InetSocketAddress(dnsIp, 53);

        final Channel ch;
        try
        {
            ch = udpABootstrap.bind(0).sync().channel();

            DnsQuery query = new DatagramDnsQuery(null, addr, randomID)
                    .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, DnsRecordType.A))
                    .setRecursionDesired(true);

            ch.writeAndFlush(query).sync().addListener(
                    new GenericFutureListener<>()
                    {
                        @Override
                        public void operationComplete(Future< ? super Void > future)
                        {
                            if( !future.isSuccess() )
                                throw new DnsException("fail send query message");
                            else if( future.isCancelled() )
                                throw new DnsException("operation cancelled");
                        }
                    }
            );

            boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);
            if( !bSuccess )
            {
                log.error("fail to resolve domain by UDP, timed out, domain : {}, dns : {}", domainName, dnsIp);
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by UDP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            log.error("fail to resolve A record, interrupted exception");
            throw new DnsException("fail to resolve A record, interrupted exception");
        }

        List<DnsResult> list = ch.pipeline().get(DnsResponseHandlerA.class).getResult();
        /*return new AsyncResult<>(list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList()));*/
        return list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList());
    }
}
