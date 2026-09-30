package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** 每个被接管内容对应一个模块；不同签名或注入策略可自行实现此接口。 */
public interface PatchModule {
    PatchSpec spec();
    MethodNode target(ClassNode node);
    void apply(ClassNode node);
}
