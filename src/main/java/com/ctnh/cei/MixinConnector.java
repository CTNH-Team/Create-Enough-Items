package com.ctnh.cei;

import com.llamalad7.mixinextras.utils.MixinInternals;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import tech.vixhentx.mcmod.ctnhlib.utils.mapping.MappingImpl;
import tech.vixhentx.mcmod.ctnhlib.utils.mapping.MappingTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
public class MixinConnector implements IMixinConnector, IMixinConfigPlugin {

    public static final boolean fastImpl = !Boolean.getBoolean("kallfix.noFastImpl");

    @Override
    public void connect() {
        log.info("add recode");
        // 格式：Map<目标类, List<mixin类>>
        Map<String, List<String>> preOverwrites = new HashMap<>();
        // 风险：中 原因： 少一次重复检查可能导致刷物品但是这东西有点闲的蛋疼
        preOverwrites.put(("dev.emi.emi.runtime.EmiReloadManager$ReloadWorker".replace('.', '/')),
                new ArrayList<>(List.of(
                        "com.ctnh.cei.falseMixin.PreReloadWorkerMixin")));
        // preOverwrites.put(ForgeAsm.minecraft_map.mapClass("net.minecraft.world.level.block.PointedDripstoneBlock".replace('.',
        // '/'))
        // , new ArrayList<>(List.of(
        // "n1luik.LinkBukkit.falseMixin.PrePointedDripstoneBlockMixin"
        // )));
        // 这里实现根据preOverwrites进行提前重写
        MixinInternals.registerExtension(new IExtension() {

            @Override
            public boolean checkActive(MixinEnvironment environment) {
                return true;// 没有提供如何定位
            }

            /**
             * 把mixin的Overwrite，不支持 @Unique
             */
            @Override
            public void preApply(ITargetClassContext context) {
                ClassNode classNode = context.getClassNode();
                var list = preOverwrites.get(classNode.name);
                if (list == null) return;
                List<ClassNode> mixins = new ArrayList<>();

                for (var mixinClass : list) {
                    // 读取他的类
                    InputStream resourceAsStream = MixinConnector.class
                            .getResourceAsStream("/" + mixinClass.replace('.', '/') + ".class");
                    if (resourceAsStream == null) {
                        log.error("MixinConnector: 无法找到mixin类{}", mixinClass);
                        continue;
                    }
                    ClassNode mixinClassNode = new ClassNode();
                    try {
                        new ClassReader(resourceAsStream).accept(mixinClassNode, 0);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String name1 = mixinClassNode.name;
                    new MappingTransformer(new MappingImpl() {

                        @Override
                        public String mapClass(String name) {
                            if (name.equals(name1)) {
                                return classNode.name;
                            }
                            return name;
                        }
                    }).transform(mixinClassNode);
                    mixins.add(mixinClassNode);
                }
                // 已经替换的函数，进行安全检查用
                Set<String> replacedMethHashSet = new HashSet<>();
                List<MethodNode> overwriteMethods = new ArrayList<>();
                for (var mixinClassNode : mixins) {
                    for (MethodNode method : mixinClassNode.methods) {
                        if (!findAnnotation(method, "Lorg/spongepowered/asm/mixin/Overwrite;")) {
                            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
                                if (!replacedMethHashSet.add(method.name + method.desc)) {
                                    log.error("MixinConnector: Mixin {} 中存在重复的Overwrite函数 {} {}", mixinClassNode.name,
                                            method.name, method.desc);
                                    continue;
                                }
                                overwriteMethods.add(method);
                            }
                            continue;
                        }
                        if (!replacedMethHashSet.add(method.name + method.desc)) {
                            log.error("MixinConnector: Mixin {} 中存在重复的Overwrite函数 {} {}", mixinClassNode.name,
                                    method.name, method.desc);
                            continue;
                        }
                        boolean found = false;
                        Iterator<MethodNode> iterator = classNode.methods.iterator();
                        while (iterator.hasNext()) {
                            MethodNode next = iterator.next();
                            if (next.name.equals(method.name) && next.desc.equals(method.desc)) {
                                method.access = next.access;
                                overwriteMethods.add(method);
                                iterator.remove();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            log.error("MixinConnector: Mixin {} 中存在Overwrite函数 {} {} 但是目标类 {} 中不存在",
                                    mixinClassNode.name, method.name, method.desc, classNode.name);
                        }
                    }
                }
                classNode.methods.addAll(overwriteMethods);
            }

            @Override
            public void postApply(ITargetClassContext context) {}

            @Override
            public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {}
        }, true);
    }

    public static boolean findAnnotation(MethodNode met, String annotation) {
        if (met.visibleAnnotations != null && met.visibleAnnotations.stream().anyMatch(a -> a.desc.equals(annotation)))
            return true;
        return met.invisibleAnnotations != null &&
                met.invisibleAnnotations.stream().anyMatch(a -> a.desc.equals(annotation));
    }

    @Override
    public void onLoad(String mixinPackage) {
        connect();
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
