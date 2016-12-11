package com.nx.util.jme3.debug;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.TangentBinormalGenerator;
import com.nx.util.jme3.base.DebugUtil;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * //TODO: Currently it can have strange effects in the scene if their geometries materials (or the materials renderstates) are changed externally once the debug is active.
 * //TODO: Solution to this: create a dupped scene and attach it to the debugNode (and set the cullhint for the uppernodes to always)
 * Created by NemesisMate on 1/12/16.
 */
public class DebugMeshState extends AbstractThreadedDebugStateModule {

    private boolean colors;
    private boolean wire;
    private boolean faceCullOff;
    private boolean normals;
//    private boolean neverCull;

//    Node selected;
//    AssetManager assetManager;


//    boolean usingOriginalMats;
    private Map<Geometry, Material> originalMats;
    private Map<Geometry, Material> clonedMats;
    private Map<Material, RenderState> originalRenderStates;


    public DebugMeshState(boolean colors, boolean wire, boolean normals, boolean faceCullOff) {
        this.colors = colors;
        this.wire = wire;
        this.normals = normals;
        this.faceCullOff = faceCullOff;
//        this.neverCull = neverCull;

        // This mode DOESN'T respect the original shaders and thus, the vertex movement done by these.
//        if(isUsingGeneratedMats()) {

//        } else {
//            originalRenderStates = new HashMap<>();
//        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        originalMats = new HashMap<>();
        clonedMats = new HashMap<>();
    }

    //    @Override
//    public void initialize(AppStateManager stateManager, Application app) {
//        super.initialize(stateManager, app);
//
////        DebugState debugState = stateManager.getState(DebugState.class);
//
////        selected = debugState.getRootNode();
////        assetManager = debugState.getAssetManager();
////        originalMats = new HashMap<>();
//    }

    @Override
    public void cleanup() {
        super.cleanup();

//        if(isUsingGeneratedMats()) {

            for(Map.Entry<Geometry, Material> entry : originalMats.entrySet()) {
                if(!entry.getKey().isGrouped()) {
                    entry.getKey().setMaterial(entry.getValue());
                }
            }
//
//        } else {
//            for(Map.Entry<Material, RenderState> entry : originalRenderStates.entrySet()) {
//                entry.getKey().getAdditionalRenderState().set(entry.getValue());
//            }
//
//        }

        originalMats = null;
        clonedMats = null;
    }

    @Override
    protected void threadCall() {

    }

    @Override
    protected boolean threadCall(Spatial spatial) {
        if(spatial instanceof Node) {
            return false;
        }

        Geometry geometry = (Geometry) spatial;

        Material material = geometry.getMaterial();

        Material clonedMat = clonedMats.get(geometry);
        if(clonedMat != null) {
            if(!clonedMat.contentEquals(material)) {
                //TODO: solve this case
                String errorMessage = "Something weird could be happened. Not change any material while mesh debug is enabled.";
                LoggerFactory.getLogger(this.getClass()).error(errorMessage);
                throw new UnsupportedOperationException(errorMessage);
            }

            return true;
        }


        originalMats.put(geometry, material);

        Material newMaterial = getDebugMaterial(geometry, colors, wire, normals, faceCullOff);
//        if(isUsingGeneratedMats()) {
////            if(originalMats.containsKey(geom)) {
////                return true;
////            }
//
////            originalMats.put(geom, geom.getMaterial());
//
//            newMaterial = DebugUtil.createDebugMaterial(geometry, colors);
//
////            app.enqueue(new Callable<Void>() {
////                @Override
////                public Void call() throws Exception {
////                    geom.setMaterial(newMaterial);
////
////                    return null;
////                }
////            });
//
//
//        } else {
////            Material material = geom.getMaterial();
////            if(originalRenderStates.containsKey(material)) {
////                return true;
////            }
//
//            newMaterial = material.clone();
//            RenderState renderState = newMaterial.getAdditionalRenderState();
//
//
//
//            renderState.setWireframe(!renderState.isWireframe());
//
//
////            RenderState renderState = material.getAdditionalRenderState();
//
////            originalRenderStates.put(material, renderState.clone());
//
////            app.enqueue(new Callable<Void>() {
////                @Override
////                public Void call() throws Exception {
////
////                    if(faceCullOff) {
////                        renderState.setFaceCullMode(RenderState.FaceCullMode.Off);
////                    }
////
////                    renderState.setWireframe(!renderState.isWireframe());
////
////                    return null;
////                }
////            });
//        }
//
//        RenderState renderState = newMaterial.getAdditionalRenderState();
//
//        if(faceCullOff) {
//            renderState.setFaceCullMode(RenderState.FaceCullMode.Off);
//        }
//        if(wire || !colors) {
//            renderState.setWireframe(!renderState.isWireframe());
//        }


        clonedMats.put(geometry, newMaterial.clone());

        app.enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if(geometry.getMesh().getBuffer(VertexBuffer.Type.Normal) != null && geometry.getMesh().getBuffer(VertexBuffer.Type.Tangent) == null) {
                    TangentBinormalGenerator.generate(geometry);
                }

                geometry.setMaterial(newMaterial);

                return null;
            }
        });


        return false;
    }


    private void checkTexCoord() {
        //TODO: use a colorgrid texture (1024x1024?) or just color each pixel with it texCoord value as color in the shader.

    }

    protected static Material getDebugMaterial(Geometry geometry, boolean colors, boolean wire, boolean normals, boolean faceCullOff) {
//        RenderState renderState = geometry.getMaterial().getAdditionalRenderState();

        Material debugMaterial;
        if(normals) {
            debugMaterial = DebugUtil.createNormalMaterial(geometry);
        }  else if(colors || !wire) {
            debugMaterial = DebugUtil.createDebugMaterial(geometry, colors);
            LoggerFactory.getLogger(DebugMeshState.class).debug("ALSKD");
        } else {
            LoggerFactory.getLogger(DebugMeshState.class).debug("asdfasd33222222");
            debugMaterial = geometry.getMaterial().clone();
            wire = true;
        }

        RenderState debugRenderState = debugMaterial.getAdditionalRenderState();

        if(wire) {
            LoggerFactory.getLogger(DebugMeshState.class).debug("noup");
            debugRenderState.setWireframe(!geometry.getMaterial().getAdditionalRenderState().isWireframe());
        }

        if(faceCullOff) {
            debugRenderState.setFaceCullMode(RenderState.FaceCullMode.Off);
        }

        return debugMaterial;
    }

//    @Override
//    protected Collection createCache() {
////        return new HashSet<>();
//        return originalMats != null ? originalMats.keySet() : super.createCache();
//    }

//    private boolean isUsingGeneratedMats() {
//        return colors || !wire;
//    }
}