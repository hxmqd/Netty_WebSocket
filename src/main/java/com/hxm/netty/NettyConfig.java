package com.hxm.netty;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @Description:存储工程的全局配置
 * @Auther: HXM
 * @Date: 2018/7/25 17:39
 */
public class NettyConfig {

    /**
     * 存储每个客户端接入时的channel对象
     */
    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
