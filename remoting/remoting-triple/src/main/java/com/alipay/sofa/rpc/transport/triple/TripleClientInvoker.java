/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Invoker for Grpc
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.12.15 7:06 PM
 */
public class TripleClientInvoker {

    private final Channel        channel;

    private final Object         request;
    private final StreamObserver responseObserver;
    private final Class          requestClass;

    private final Method         method;
    private final String[]       methodArgSigs;
    private final Object[]       methodArgs;

    private final String         serviceName;
    private final String         interfaceName;

    private final Integer        timeout;

    private final static Logger  LOGGER = LoggerFactory.getLogger(TripleClientInvoker.class);

    /**
     * The constructor
     *
     * @param sofaRequest The SofaRequest
     * @param channel     The Channel
     */
    public TripleClientInvoker(SofaRequest sofaRequest, Channel channel) {
        this.channel = channel;
        this.method = sofaRequest.getMethod();
        this.methodArgs = sofaRequest.getMethodArgs();
        this.methodArgSigs = sofaRequest.getMethodArgSigs();
        this.interfaceName = sofaRequest.getInterfaceName();
        this.serviceName = interfaceName.substring(0, interfaceName.indexOf('$'));
        this.request = methodArgs[0];
        this.responseObserver = methodArgs.length == 2 ? (StreamObserver) methodArgs[1] : null;
        this.requestClass = ClassUtils.forName(methodArgSigs[0]);

        try {
            requestClass.cast(request);
        } catch (ClassCastException e) {
            throw e;
        }
        this.timeout = sofaRequest.getTimeout();
    }

    public SofaResponse invoke(ConsumerConfig consumerConfig, int timeout)
        throws Exception {
        SofaResponse sofaResponse = new SofaResponse();
        Object response = invokeRequestMethod(consumerConfig, timeout);
        sofaResponse.setAppResponse(response);
        return sofaResponse;
    }

    private CallOptions buildCallOptions() {
        CallOptions callOptions = CallOptions.DEFAULT;
        if (timeout != null) {
            callOptions = callOptions.withDeadlineAfter(timeout, TimeUnit.SECONDS);
        }
        return callOptions;
    }

    public Object invokeRequestMethod(ConsumerConfig consumerConfig, int timeout)
        throws Exception {
        Object appResponse = null;
        Class enclosingClass = consumerConfig.getProxyClass().getEnclosingClass();

        Method sofaStub = enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class,
            ProviderInfo.class, ConsumerConfig.class, int.class);
        Object stub = sofaStub.invoke(null, channel, CallOptions.DEFAULT, null, null, timeout);

        appResponse = method.invoke(stub, methodArgs[0]);

        return appResponse;
    }

}