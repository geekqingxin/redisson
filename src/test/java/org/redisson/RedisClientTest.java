package org.redisson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.LongCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.StringCodec;

import io.netty.util.concurrent.Promise;

public class RedisClientTest {

    @Test
    public void test() throws InterruptedException {
        RedisClient c = new RedisClient("localhost", 6379);
        final RedisConnection conn = c.connect();

        conn.sync(StringCodec.INSTANCE, RedisCommands.SET, "test", 0);
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
        for (int i = 0; i < 100000; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    conn.async(StringCodec.INSTANCE, RedisCommands.INCR, "test");
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);

        Assert.assertEquals(100000L, conn.sync(LongCodec.INSTANCE, RedisCommands.GET, "test"));
    }

    @Test
    public void testPipeline() throws InterruptedException, ExecutionException {
        RedisClient c = new RedisClient("localhost", 6379);
        RedisConnection conn = c.connect();

        conn.sync(StringCodec.INSTANCE, RedisCommands.SET, "test", 0);

        List<CommandData<?, ?>> commands = new ArrayList<CommandData<?, ?>>();
        CommandData<String, String> cmd1 = conn.create(null, RedisCommands.PING);
        commands.add(cmd1);
        CommandData<Long, Long> cmd2 = conn.create(null, RedisCommands.INCR, "test");
        commands.add(cmd2);
        CommandData<Long, Long> cmd3 = conn.create(null, RedisCommands.INCR, "test");
        commands.add(cmd3);
        CommandData<String, String> cmd4 = conn.create(null, RedisCommands.PING);
        commands.add(cmd4);

        conn.send(commands);

        Assert.assertEquals("PONG", cmd1.getPromise().get());
        Assert.assertEquals(1, (long)cmd2.getPromise().get());
        Assert.assertEquals(2, (long)cmd3.getPromise().get());
        Assert.assertEquals("PONG", cmd4.getPromise().get());
    }


}
