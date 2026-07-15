package com.gly091020.SableRagdollLib.editor.view;

import com.gly091020.SableRagdollLib.editor.api.ModelSceneManager;
import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModelTreeView extends View {
    private final ModelView modelView;
    public ModelTreeView(ModelView modelView){
        this.modelView = modelView;
    }

    public void initTree(){
        clearAllChildren();
        var editor = modelView.editor;
        if(editor.getCurrentProjectFile() == null)return;
        var supplier = ModelSceneManager.get(modelView.editor.getRagdollProject().file.getType());
        if(supplier == null)return;
        var modelID = supplier.getModel(editor.getCurrentProjectFile().toPath());
        if(modelID == null)return;

        var root = new TreeNode<String, String>("__modelParent__");
        supplier.buildModelTree(modelID, root);
        var treeList = new TreeList<>(root);
        treeList.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        treeList.setNodeUISupplier(TreeList.textTemplate(node -> Component.literal(node.getKey())));
        treeList.setFlattenRoot(true);
        treeList.setSupportMultipleSelection(true);
        treeList.expandNode(root);
        treeList.setSelected(List.of(), false);
        addChild(treeList);

        treeList.setOnSelectedChanged(treeNodes -> {
            var o = modelView.getSceneObject();
            if(o == null)return;
            o.setParts(treeNodes.stream().map(
                    treeNode -> new RagdollRenderData.EveryPart(treeNode.getKey(), false)
            ).toList());
        });
    }
}
