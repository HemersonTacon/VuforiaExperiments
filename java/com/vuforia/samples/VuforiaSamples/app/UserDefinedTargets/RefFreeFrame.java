/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.UserDefinedTargets;

import android.util.Log;

import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.Renderer;
import com.vuforia.TrackableSource;
import com.vuforia.TrackerManager;
import com.vuforia.Vec2F;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;


public class RefFreeFrame
{
    
    private static final String LOGTAG = "RefFreeFrame";
    
    // Some helper functions
    enum STATUS
    {
        STATUS_IDLE, STATUS_SCANNING, STATUS_CREATING, STATUS_SUCCESS
    };
    
    STATUS curStatus;
    
    // / Current color of the target finder. This changes color
    // / depending on frame quality.
    float colorFrame[];
    
    // / Half of the screen size, used often in the rendering pipeline
    Vec2F halfScreenSize;
    
    // / Keep track of the time between frames for color transitions
    long lastFrameTime;
    long lastSuccessTime;
    
    // All rendering methods are contained in this class for easy
    // extraction/abstraction
    RefFreeFrameGL frameGL;
    
    // The latest trackable source to be extracted from the Target Builder
    TrackableSource trackableSource;
    
    UserDefinedTargets mActivity;
    
    SampleApplicationSession vuforiaAppSession;
    
    // funcao para alterar os valores de cor, eh utilizada para deixar o viewfinder verde
    // se a soma do 2 primeiros parametros sair fora do range, eh retornado um dos limites do range
    // Function used to transition in the range [0, 1]
    float transition(float v0, float inc, float a, float b)
    {
        float vOut = v0 + inc;
        return (vOut < a ? a : (vOut > b ? b : vOut));
    }

    // funcao para alterar os valores do color frame (nao entendi muito bem pra que
    // serve essa alteracao das cores ainda)
    float transition(float v0, float inc)
    {
        return transition(v0, inc, 0.0f, 1.0f);
    }
    
    // classe que auxilia na renderizacao da cena
    public RefFreeFrame(UserDefinedTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
        colorFrame = new float[4];
        curStatus = STATUS.STATUS_IDLE;
        lastSuccessTime = 0;
        trackableSource = null;
        colorFrame[0] = 1.0f;
        colorFrame[1] = 0.0f;
        colorFrame[2] = 0.0f;
        colorFrame[3] = 0.75f;
        
        frameGL = new RefFreeFrameGL(mActivity, vuforiaAppSession);
        halfScreenSize = new Vec2F();
    }

    // carrega as texturas  de marcas (que eu ainda nao vi sendo usadas)
    // e inicia o trackable como null
    void init()
    {
        // load the frame texture
        frameGL.getTextures();
        
        trackableSource = null;
    }
    
    // para o escaneamento
    void deInit()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
            .getTracker(ObjectTracker.getClassType()));
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            if (targetBuilder != null
                && (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE))
            {
                targetBuilder.stopScan();
            }
        }
    }
    
    // incializa coisas do opengl e atualiza o tamanho do video e atualiza o status para idle
    // eh chamado quando as dimensoes da tela sao alteradas
    void initGL(int screenWidth, int screenHeight)
    {
        // inicializa as coisas do opengl necessarias para renderizar a cena
        frameGL.init(screenWidth, screenHeight);
        
        Renderer renderer = Renderer.getInstance();
        VideoBackgroundConfig vc = renderer.getVideoBackgroundConfig();
        int temp[] = vc.getSize().getData();
        float[] videoBackgroundConfigSize = new float[2];
        videoBackgroundConfigSize[0] = temp[0] * 0.5f;
        videoBackgroundConfigSize[1] = temp[1] * 0.5f;
        
        halfScreenSize.setData(videoBackgroundConfigSize);
        
        // sets last frame timer
        lastFrameTime = System.currentTimeMillis();
        
        reset();
    }
    
    // muda o status para idle
    void reset()
    {
        curStatus = STATUS.STATUS_IDLE;
        
    }
    
    // muda o status para creating, eh chamado quando o botao da camera eh pressionado
    void setCreating()
    {
        curStatus = STATUS.STATUS_CREATING;
    }
    
    // parece atualizar o status de criacao para sucesso apos o usuario pressionar o botao da
    // camera e tambem alterna a cor dos marcadores entre branco e verde de acordo com a
    // qualidade do frame
    void updateUIState(ImageTargetBuilder targetBuilder, int frameQuality)
    {
        // ** Elapsed time
        long elapsedTimeMS = System.currentTimeMillis() - lastFrameTime;
        lastFrameTime += elapsedTimeMS;
        
        // This is a time-dependent value used for transitions in 
        // the range [0,1] over the period of half of a second.
        float transitionHalfSecond = elapsedTimeMS * 0.002f;
        
        STATUS newStatus = curStatus;
        
        switch (curStatus)
        {
            // pelo que vi esse status ocorre quando nao esta ocorrendo nenhum processo de
            // escaneamento e apos o status mudar pra sucesso e o tracker ser inicializado
            case STATUS_IDLE:
                if (frameQuality != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    newStatus = STATUS.STATUS_SCANNING;
                
                break;
            // esse status parece sempre estar settado apos a inicializacao da vuforia
            case STATUS_SCANNING:
                switch (frameQuality)
                {
                // bad target quality, render the frame white until a match is
                // made, then go to green
                    case ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW:
                        colorFrame[0] = 1.0f;
                        colorFrame[1] = 1.0f;
                        colorFrame[2] = 1.0f;
                        
                        break;
                    
                    // good target, switch to green over half a second
                    // quando encontra um bom frame, os valores de vermelho e azul vao diminuindo
                    // com o passar do tempo em que o bom frame se mantem na tela enquanto o
                    // valor do verde vai aumentando
                    case ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_HIGH:
                    case ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_MEDIUM:
                        colorFrame[0] = transition(colorFrame[0], -transitionHalfSecond);
                        colorFrame[1] = transition(colorFrame[1],  transitionHalfSecond);
                        colorFrame[2] = transition(colorFrame[2], -transitionHalfSecond);
                        
                        break;
                }
                break;
            // esse status acontece quando o botao da camera eh pressionado
            case STATUS_CREATING:
            {
                // check for new result
                // if found, set to success, success time and:
                TrackableSource newTrackableSource = targetBuilder.getTrackableSource();
                if (newTrackableSource != null)
                {
                    newStatus = STATUS.STATUS_SUCCESS;
                    lastSuccessTime = lastFrameTime;
                    trackableSource = newTrackableSource;

                    //muda o satus para idle e remove o anel de loading da tela
                    mActivity.targetCreated();
                }
            }
            default:
                break;
        }
        
        curStatus = newStatus;
    }
    
    // atualiza o status, chama o metodo para inicializar os trackers (doStartTrackers
    // da atividade)quando o status eh succes e chama o metodo renderScanningViewfinder
    // quando o status eh scanning para mostrar os marcadores que indicam a qualidade do frame
    void render()
    {
        // Get the image tracker
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager.getTracker(ObjectTracker.getClassType()));
        
        // Get the frame quality from the target builder
        ImageTargetBuilder targetBuilder = objectTracker.getImageTargetBuilder();
        int frameQuality = targetBuilder.getFrameQuality();
        
        // Update the UI internal state variables
        // atualiza a variavel de status e a cor dos marcadores
        updateUIState(targetBuilder, frameQuality);
        
        if (curStatus == STATUS.STATUS_SUCCESS)
        {
            curStatus = STATUS.STATUS_IDLE;
            
            Log.d(LOGTAG, "Built target, reactivating dataset with new target");
            mActivity.doStartTrackers();
        }
        
        // Renders the hints
        // se o status for scanning os marcadores sao mostrados na tela
        switch (curStatus)
        {
            case STATUS_SCANNING:
                renderScanningViewfinder(frameQuality);
                break;
            default:
                break;
        
        }
        
        SampleUtils.checkGLError("RefFreeFrame render");
    }
    
    // setta as matrizes de escala e cor que serao usadas na renderizacao do viewfinder
    // (marcadores), e rendereiza de fato o viewfinder
    void renderScanningViewfinder(int quality)
    {
        frameGL.setModelViewScale(1.0f);
        frameGL.setColor(colorFrame);
        frameGL.renderViewfinder();
    }
    
    // verifica se existe novo trackable
    boolean hasNewTrackableSource()
    {
        return (trackableSource != null);
    }
    
    // retorna o trackable e setta o trackableSource para null novamente
    TrackableSource getNewTrackableSource()
    {
        TrackableSource result = trackableSource;
        trackableSource = null;
        return result;
    }
}
