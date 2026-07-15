package com.gly091020.SableRagdollLib.editor.api;

import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public interface IModelSceneSupplier {
    ResourceLocation getModel(Path path);
    void buildModelTree(ResourceLocation resourceLocation, TreeNode<String, String> root);
    AbstractModelSceneObject createNewModelObject(ResourceLocation resourceLocation);
}
