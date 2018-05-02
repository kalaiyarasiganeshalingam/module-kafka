/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.kafka.nativeimpl.consumer.action;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.ballerinalang.kafka.util.KafkaConstants.CONSUMER_STRUCT_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.KAFKA_NATIVE_PACKAGE;
import static org.ballerinalang.kafka.util.KafkaConstants.NATIVE_CONSUMER;
import static org.ballerinalang.kafka.util.KafkaConstants.OFFSET_STRUCT_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.ORG_NAME;
import static org.ballerinalang.kafka.util.KafkaConstants.PACKAGE_NAME;

/**
 * Native function commits given offsets of consumer to offset topic.
 */
@BallerinaFunction(
        orgName = ORG_NAME,
        packageName = PACKAGE_NAME,
        functionName = "commitOffset",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = CONSUMER_STRUCT_NAME,
                structPackage = KAFKA_NATIVE_PACKAGE),
        args = {
                @Argument(name = "offsets", type = TypeKind.ARRAY, elementType = TypeKind.STRUCT,
                        structType = OFFSET_STRUCT_NAME, structPackage = KAFKA_NATIVE_PACKAGE)
        },
        isPublic = true)
public class CommitOffset implements NativeCallableUnit {

    @Override
    public void execute(Context context, CallableUnitCallback callableUnitCallback) {
        BStruct consumerStruct = (BStruct) context.getRefArgument(0);
        KafkaConsumer<byte[], byte[]> kafkaConsumer = (KafkaConsumer) consumerStruct
                .getNativeData(NATIVE_CONSUMER);

        if (Objects.isNull(kafkaConsumer)) {
            throw new BallerinaException("Kafka Consumer has not been initialized properly.");
        }

        BRefValueArray offsets = ((BRefValueArray) context.getRefArgument(1));
        Map<TopicPartition, OffsetAndMetadata> partitionToMetadataMap = new HashMap<>();

        BStruct offset;
        BStruct partition;
        int offsetValue;
        String topic;
        int partitionValue;

        if (Objects.nonNull(offsets)) {
            for (int counter = 0; counter < offsets.size(); counter++) {
                offset = (BStruct) offsets.get(counter);
                partition = (BStruct) offset.getRefField(0);
                offsetValue = new Long(offset.getIntField(0)).intValue();
                topic = partition.getStringField(0);
                partitionValue = new Long(partition.getIntField(0)).intValue();
                partitionToMetadataMap.put(new TopicPartition(topic, partitionValue),
                                           new OffsetAndMetadata(offsetValue));
            }
        }

        try {
            kafkaConsumer.commitSync(partitionToMetadataMap);
        } catch (IllegalArgumentException | KafkaException e) {
            throw new BallerinaException("Failed to commit offsets. " + e.getMessage(), e, context);
        }
    }

    @Override
    public boolean isBlocking() {
        return true;
    }
}