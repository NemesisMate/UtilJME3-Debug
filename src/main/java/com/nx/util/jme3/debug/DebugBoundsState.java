package com.nx.util.jme3.debug;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Sphere;
import com.nx.util.jme3.base.SceneVars;
import com.nx.util.jme3.base.SpatialUtil;

/**
 * Created by NemesisMate on 1/12/16.
 */
public class DebugBoundsState extends AbstractDebugGraphStateModule {

//    private final String DEBUG_BOUNDS_NAME = "-_Debug||Bounds_-";
    private final static Vector3f NO_VOLUME_SIZE = Vector3f.UNIT_XYZ.mult(0.5f);
    private final static Vector3f GEOM_CENTER_SIZE = Vector3f.UNIT_XYZ.mult(0.5f);

    private boolean nodeBounds;
    /**
     * If true, it will show the node positions even when nodes are no empty.
     */
    private boolean alwaysMarkNodePosition;

    private boolean markGeomsPosition;
    private boolean markGeomsCenter;

    private Material geometriesMaterial;
    private Material nodesMaterial;
    private Material emptyNodesMaterial;
    private Material noVolumeNodeMaterial;
    private Material noVolumeGeomMaterial;

    private Material geomCenterMaterial;


//    private Map<Spatial, Geometry> debuggedBounds = new ConcurrentHashMap<>();
//    SimpleBatchNode debugsNode = new SimpleBatchNode(DEBUG_BOUNDS_NAME);
//    Node debugsNode = new Node(DEBUG_BOUNDS_NAME);

    private Vector3f aux = new Vector3f();



    public DebugBoundsState(boolean nodeBounds, boolean alwaysMarkNodePosition) {
        super(true);

        this.nodeBounds = nodeBounds;
        this.alwaysMarkNodePosition = alwaysMarkNodePosition;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.geometriesMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Green);
        this.noVolumeGeomMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Blue);

        this.nodesMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Yellow);
        this.noVolumeNodeMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Orange);

        this.emptyNodesMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Red);

        this.geomCenterMaterial = SpatialUtil.createMaterial(getAssetManager(), ColorRGBA.Magenta);


        this.geometriesMaterial.getAdditionalRenderState().setWireframe(true);
        this.noVolumeGeomMaterial.getAdditionalRenderState().setWireframe(true);
        this.nodesMaterial.getAdditionalRenderState().setWireframe(true);
        this.noVolumeNodeMaterial.getAdditionalRenderState().setWireframe(true);
        this.emptyNodesMaterial.getAdditionalRenderState().setWireframe(true);

    }

    @Override
    protected boolean update(Spatial spatial, float tpf) {
        if (debugContains(spatial)) {
            return false;
        }

        if (spatial instanceof Node) {

            if (((Node) spatial).getQuantity() == 0 || alwaysMarkNodePosition) {
                Vector3f worldScale = spatial.getWorldScale();


//                                        Mesh s = new Dome(null, 2, 10, 0.25f * worldScale.length(), false);
//                                        Mesh s = new Cylinder(10, 1, 0.25f * worldScale.length(), 0.25f * worldScale.length(), true);

                Mesh finalS = new Sphere(7, 7, 0.25f * worldScale.length());

//                                        Mesh s = new Sphere(7, 7, 0.25f * worldScale.length());
//                                        Geometry geom = new Geometry("DU Created Sphere", s);
//
//                                        Mesh s2 = new Arrow(new Vector3f(0, 0.25f * worldScale.length(), 0));
//                                        Geometry geom2 = new Geometry("DU Created Arrow", s2);
//
//                                        List<Geometry> geoms = new ArrayList<Geometry>(2) {{
//                                            add(geom); add(geom2);
//                                        }};
//
//                                        Mesh finalS = new Mesh();
//                                        GeometryBatchFactory.mergeGeometries(geoms, finalS);

                Geometry debugGeometry = new Geometry("DU Created Compound", finalS);


                addboundDebug(spatial, debugGeometry, true);
//                                    debuggedBounds.put(task, debugGeometry);
            }
        }

        if (spatial instanceof Geometry || nodeBounds) {

            Geometry debugGeometry = null;

            BoundingVolume bv = spatial.getWorldBound();

            if(bv == null) {
                debugGeometry = SpatialUtil.createBox(NO_VOLUME_SIZE);
            } else {
                if (bv instanceof BoundingBox) {
                    BoundingBox bb = (BoundingBox) bv;

                    if(bv.getVolume() == 0) {
                        debugGeometry = SpatialUtil.createBox(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
                    } else {
                        debugGeometry = SpatialUtil.createBox(NO_VOLUME_SIZE);
                    }
                    //                                        debuggedBounds.put(task, debugGeometry);
                } else if (bv instanceof BoundingSphere) {
                    BoundingSphere bs = (BoundingSphere) bv;
                    if(bv.getVolume() == 0) {
                        debugGeometry = SpatialUtil.createSphere(bs.getRadius());
                    } else {
                        debugGeometry = SpatialUtil.createSphere(NO_VOLUME_SIZE);
                    }
                    //                                        debuggedBounds.put(task, debugGeometry);
                } else {
                    new UnsupportedOperationException("Bounding volume type not supported: " + bv.getClass()).printStackTrace();
                }
            }




            if(debugGeometry != null) {
                addboundDebug(spatial, debugGeometry, false);

            }
        }


        return false;
    }


    private void addboundDebug(final Spatial originalSpatial, final Geometry debugGeometry, boolean locationMark) {
//        final Spatial key = originalSpatial;


        if(locationMark) {
            debugGeometry.setMaterial(((Node) originalSpatial).getQuantity() == 0 ? emptyNodesMaterial : nodesMaterial);

            debugGeometry.addControl(new AbstractControl() {
                @Override
                protected void controlUpdate(float tpf) {
                    if (originalSpatial.getParent() == null) {
                        removeFromDebug(originalSpatial);

                        // Patch to ensure this geom is removed (as we add more than one for task)
                        spatial.removeFromParent();
                    } else {
                        Geometry geometry = (Geometry) this.spatial;

                        if(geometry.getMaterial() == emptyNodesMaterial) {
                            if(((Node) originalSpatial).getQuantity() != 0) {
                                changeDebugMaterial(geometry, nodesMaterial);
                            }
                        } else {
                            if(((Node) originalSpatial).getQuantity() == 0) {
                                changeDebugMaterial(geometry, emptyNodesMaterial);
                            }
                        }

                        // This check is very important when batching
                        if(!spatial.getLocalTransform().equals(originalSpatial.getWorldTransform())) {
                            spatial.setLocalTransform(originalSpatial.getWorldTransform());
                        }
                    }
                }

                @Override
                protected void controlRender(RenderManager rm, ViewPort vp) {

                }
            });
        } else {
            debugGeometry.setMaterial(originalSpatial instanceof Node ? nodesMaterial : geometriesMaterial);
            debugGeometry.addControl(new AbstractControl() {
                @Override
                protected void controlUpdate(float tpf) {
                    SceneVars tempVars = SceneVars.get();
                    Vector3f scale = tempVars.vect1;

                    Geometry geometry = (Geometry) this.spatial;
                    //                                        Spatial key = task;

                    scale.set(geometry.getLocalScale());

                    //                LoggerFactory.getLogger(this.getClass()).debug("Debugging bound for node: {}.", key);

                    if (originalSpatial.getParent() == null) {
                        removeFromDebug(originalSpatial);

                        // Patch to ensure this geom is removed (as we add more than one for task)
                        spatial.removeFromParent();

                        tempVars.release();
                        return;
                    } else {
                        BoundingVolume bv = originalSpatial.getWorldBound();
                        BoundingVolume bv2 = spatial.getWorldBound();

                        //                Vector3f localScale = value.getLocalScale();

                        Vector3f newLocation;
                        if (bv != null && bv.getVolume() != 0) {
                            newLocation = bv.getCenter().subtract(originalSpatial.getWorldTranslation(), aux).addLocal(originalSpatial.getWorldTranslation());

                            if(originalSpatial instanceof Node) {
                                changeDebugMaterial(debugGeometry, nodesMaterial);
                            } else {
                                changeDebugMaterial(debugGeometry, geometriesMaterial);
                            }


                            if (bv instanceof BoundingBox) {
                                BoundingBox bb = (BoundingBox) bv;
                                BoundingBox bb2 = (BoundingBox) bv2;

                                scale.setX(scale.getX() + (bb.getXExtent() - bb2.getXExtent()));
                                scale.setY(scale.getY() + (bb.getYExtent() - bb2.getYExtent()));
                                scale.setZ(scale.getZ() + (bb.getZExtent() - bb2.getZExtent()));

//                                LoggerFactory.getLogger(this.getClass()).debug("SETTING SCALE: {}, bb: {}, bb2: {}", scale, ((BoundingBox) bv).getXExtent(), bb2.getXExtent());
                            } else if (bv instanceof BoundingSphere) {
                                BoundingSphere bs = (BoundingSphere) bv;
                                BoundingBox bb2 = (BoundingBox) bv2;

                                if (bs.getRadius() != bb2.getXExtent()) {
                                    float scaleValue = bs.getRadius() - bb2.getXExtent();
                                    //                            value.setLocalScale(localScale.addLocal(scaleValue, scaleValue, scaleValue));
                                    scale.addLocal(scaleValue, scaleValue, scaleValue);
                                }
                            } else {
                                new UnsupportedOperationException("Bounding volume type not supported: " + bv.getClass()).printStackTrace();
                            }
                        } else {
                            newLocation = originalSpatial.getWorldTranslation();
                            scale.set(NO_VOLUME_SIZE);

                            if(originalSpatial instanceof Node) {
                                changeDebugMaterial(debugGeometry, noVolumeNodeMaterial);
                            } else {
                                changeDebugMaterial(debugGeometry, noVolumeGeomMaterial);
                            }
                        }

                        // This check is very important when batching
                        if (!geometry.getLocalTranslation().equals(newLocation)) {
                            geometry.setLocalTranslation(newLocation);
                        }
//                            BoundingBox bb2 = (BoundingBox) bv2;
//                            if (bb2.getXExtent() != originalSpatial.getWorldScale().length()) {
//                                float scaleValue = originalSpatial.getWorldScale().getX() - bb2.getXExtent();
//                                //                        value.setLocalScale(localScale.addLocal(scaleValue, scaleValue, scaleValue));
//                                scale.addLocal(scaleValue, scaleValue, scaleValue);
//                            }

                        // This check is very important when batching
                        if (!geometry.getLocalScale().equals(scale)) {
                            geometry.setLocalScale(scale);
                        }
                    }

                    tempVars.release();
                }

                @Override
                protected void controlRender(RenderManager rm, ViewPort vp) {

                }
            });
        }

        debugGeometry.getMesh().setStatic();

        addToDebug(originalSpatial, debugGeometry);

    }


}
