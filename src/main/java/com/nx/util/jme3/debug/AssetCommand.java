package com.nx.util.jme3.debug;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jme3.app.LegacyApplication;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.nx.util.jme3.lemur.ConsoleCommand;
import com.simsilica.lemur.Command;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TODO: This is currently bad threaded. Do it well, and make it in a way it is closed if the app is closed O.o.
 * TODO: Better just move to a DebugState as every other debug things.
 * Created by NemesisMate.
 */
public class AssetCommand implements Command<ConsoleCommand> {

    LegacyApplication app;
    AssetManager assetManager;
    Spatial selected;

    ScheduledExecutorService service;

    private enum ReloadType {
        STENCIL, ALL
    }

    private interface ReloadCondition {
        boolean meetCondition(Spatial spatial);
    }

    public AssetCommand(LegacyApplication app, Spatial selected, AssetManager assetManager) {
        this.app = app;
        this.assetManager = assetManager;
        this.selected = selected;
    }

    @Override
    public void execute(ConsoleCommand source) {
        String[] args = source.getArgs();

        LoggerFactory.getLogger(this.getClass()).debug("Executing assets command.");
        if(args != null && args.length > 0) {
            switch(args[0].toLowerCase()) {
                case "r":
                case "reload":
                    ReloadType type = null;
                    if(args.length > 1) {
                        switch (args[1].toLowerCase()) {
                            case "s":
                            case "stencil":
                            case "stencils":
                                type = ReloadType.STENCIL;
                                break;
                            case "a":
                            case "all":
                                type = ReloadType.ALL;
                                break;
                        }


                        boolean auto = false;
                        if(args.length > 2) {
                            switch (args[2].toLowerCase()) {
                                case "auto":
                                case "automatic":
                                    auto = true;
                                    break;
                            }
                        }


                        if(type != null) {
                            reload(type, auto);
                            return;
                        }

                        return;
                    }

                    reload(ReloadType.ALL, false);

                    break;
            }
        }

        return;
    }


    private void reload(ReloadType type, boolean auto) {
        Runnable runnable;
        ReloadCondition reloadCondition = null;


        switch (type) {
            case STENCIL:
                final MaterialDef matDef12 = (MaterialDef)assetManager.loadAsset(new AssetKey("Common/MatDefs/Terrain/TerrainLighting.j3md"));
                final MaterialDef matDef3 = (MaterialDef)assetManager.loadAsset(new AssetKey("Common/MatDefs/Terrain/Terrain.j3md"));

                reloadCondition = new ReloadCondition() {
                    @Override
                    public boolean meetCondition(Spatial spatial) {
                        if(spatial instanceof Geometry) {
                            Material mat = ((Geometry) spatial).getMaterial();
                            if(mat.getMaterialDef() == matDef3 || mat.getMaterialDef() == matDef12) {
                                return true;
                            }
                        }

                        return false;
                    }
                };
                break;
            case ALL:
                reloadCondition = null;
                break;

        }

        final ReloadCondition[] reloadConditions = reloadCondition != null ? new ReloadCondition[] { reloadCondition } : null;

        runnable = new Runnable() {
            @Override
            public void run() {
                MDC.clear();
//                AssetManager assetManager = app.getAssetManager();

                Set<MaterialKey> reloadedMats = new HashSet<>();

                selected.depthFirstTraversal(new SceneGraphVisitor() {
                    @Override
                    public void visit(Spatial spatial) {
                        if(reloadConditions != null) {
                            for(ReloadCondition reloadCondition : reloadConditions) {
                                if(!reloadCondition.meetCondition(spatial)) {
                                    return;
                                }
                            }
                        }


                        if(spatial instanceof Geometry) {

                            Material mat = ((Geometry) spatial).getMaterial();
                            MaterialKey matKey = (MaterialKey) mat.getKey();

                            if(matKey == null) {
                                LoggerFactory.getLogger(this.getClass()).warn("Couldn't reload material because of it key is null.");
                                return;
                            }

                            if(!reloadedMats.contains(matKey)) {
                                reloadedMats.add(matKey);

                                for(MatParam matParam : mat.getParams()) {
                                    if(matParam instanceof MatParamTexture) {
                                        TextureKey texKey = (TextureKey) ((MatParamTexture) matParam).getTextureValue().getKey();
                                        if(!assetManager.deleteFromCache(texKey)) {
                                            LoggerFactory.getLogger(this.getClass()).warn("Couldn't reload texture: {}.", texKey);
                                        }
                                    }
                                }

                                if(!assetManager.deleteFromCache(matKey)) {
                                    LoggerFactory.getLogger(this.getClass()).warn("Couldn't reload material: {}.", matKey);
                                }
                            }


                            Material material = null;
                            try {
                                material = assetManager.loadAsset(matKey);
                            } catch (Exception e) {

                            }


                            if(material != null) {
                                final Material finalMat = material;

                                app.enqueue(new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        try {
                                            spatial.setMaterial(finalMat);
                                        } catch (Exception e) {
                                            //TODO: inform about great assets or not so great
                                        }

                                        return null;
                                    }
                                });
                            }
                        }
                    }
                });
            }
        };


        if(auto) {

            if(service != null) {
                service.shutdown();
//                reloadThread.interrupt();
//                reloadThread = null;
                service = null;
                ((Logger)LoggerFactory.getLogger(this.getClass())).setLevel(Level.DEBUG);
            } else {
                service = Executors.newSingleThreadScheduledExecutor();
                service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
                ((Logger)LoggerFactory.getLogger(this.getClass())).setLevel(Level.ERROR);
//                reloadThread = new Thread(runnable);
//                reloadThread.start();
            }

            LoggerFactory.getLogger(this.getClass()).info(MarkerFactory.getMarker("CONSOLE"), "Asset automatic realoading enabled: {}", (service != null ? "true" : "false"));
        } else {
            runnable.run();
        }


    }

}
