package com.oy.oypicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 图片编辑事件 Disruptor配置，配置一个Disruptor实例，把事件和消费者串起来
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;//消费者

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        //定义ringBuffer环形数组的大小
        int bufferSize = 1024 * 256;

        //创建Disruptor实例
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,//事件工厂，即告诉Disruptor要处理的事件是什么类型，启动时，Disruptor在Ring Buffer的每个位置预创建了空的PictureEditEvent对象
                bufferSize,//缓冲区大小
                ThreadFactoryBuilder.create()//线程工厂
                        .setNamePrefix("pictureEditEventDisruptor")//线程名称前缀
                        .build()
        );

        //设置消费者，即由哪个消费者来处理事件
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        //开启disruptor
        disruptor.start();
        return disruptor;
    }
}
