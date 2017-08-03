/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.UserDefinedTargets;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleAppRenderer;
import com.vuforia.samples.SampleApplication.SampleAppRendererControl;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.vuforia.samples.SampleApplication.utils.MyGrid;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Teapot;
import com.vuforia.samples.SampleApplication.utils.Texture;

// classe que controla a renderizacao da cena
// The renderer class for the ImageTargetsBuilder sample. 
public class UserDefinedTargetRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "UDTRenderer";

    private SampleApplicationSession vuforiaAppSession;
    private SampleAppRenderer mSampleAppRenderer;

    private boolean mIsActive = false;
    
    private Vector<Texture> mTextures;
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    // Constants:
    static final float kObjectScale = 3.f;
    
    private Teapot mTeapot;
    private MyGrid myGrid;
    
    // Reference to main activity
    private UserDefinedTargets mActivity;

    // metodo construtor que salva uma referencia para a atividade e para a sessao
    public UserDefinedTargetRenderer(UserDefinedTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        // cria uma instancia da classe SampleAppRenderer passando como parametros um
        // SampleAppRendererControl que no caso eh esta classe, a atividade, um deviceMode,
        // um booleano indicando se sera stereo ou nao (ainda nao sei pra que isso serve)
        // e os planos proximo e distante da camera
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 10f, 5000f);
    }
    
    
    // Called when the surface is created or recreated.
    // inicializa o environment do openGL
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        // cria alguns recursos de renderizacao internos do vuforia
        vuforiaAppSession.onSurfaceCreated();
        // faz a inicializacao do ambiente do openGL
        // o que conta com a criacao do programa shader a patir dos codigos em
        // SampleApplication.utils.VideoBackgroundShader e referenciamento as variaveis criadas
        // nesses codigos shader
        mSampleAppRenderer.onSurfaceCreated();
    }
    
    
    // Called when the surface changed size.
    // quando as dimensoes da tela sao alteradas essa funcao
    // eh chamada para alterar algumas configuracoes
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call function to update rendering when render surface
        // parameters have changed:
        mActivity.updateRendering();
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }

    // setta flag que informa que a renderizacao ta ativa e configura o video
    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            //configura pro modo de video e ajusta a imagem da camera de acordo com a razao de aspecto
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called to draw the current frame.
    // callback chamada em cada ciclo para renderizar o frame
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    // verifica se existe um trackable na cena, no qual o teapot sera renderizado
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        // renderiza o video como plano de fundo
        mSampleAppRenderer.renderVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Render the RefFree UI elements depending on the current state
        // atualiza o status: se ele for success, inicializa os trackers; se ele for scanning,
        // mostra o viewfinder
        mActivity.refFreeFrame.render();
        
        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(trackableResult.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            float[] modelViewProjection = new float[16];
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, kObjectScale);
            Matrix.scaleM(modelViewMatrix, 0, kObjectScale, kObjectScale, kObjectScale);
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);
            
            GLES20.glUseProgram(shaderProgramID);
            // AQUI NESSAS LINHAS EH QUE PARECE QUE O TEAPOT EH SETTADO PARA SER RENDEREIZADO NO TRACKABLE
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mTeapot.getVertices());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTeapot.getTexCoords());
            
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTeapot.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, mTeapot.getIndices());
            
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            /* ***************
            TENATIVA DE DESENHAR UM GRID
            ***************** */
            GLES20.glLineWidth(3);
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, myGrid.getVertices());
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glDrawElements(GLES20.GL_LINES, myGrid.getNumObjectIndex(),
                    GLES20.GL_UNSIGNED_SHORT, myGrid.getIndices());
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glLineWidth(1);

            /* ***************
            TENATIVA DE DESENHAR OS 3 EIXOS
            ***************** */

            float[] X_axis = {
                    0, 0, 0,
                    50, 0, 0
            };
            float[] Y_axis = {
                    0, 0, 0,
                    0, 50, 0
            };
            float[] Z_axis = {
                    0, 0, 0,
                    0, 0, 50
            };
            /*GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, );
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glDrawElements(GLES20.GL_LINES, myGrid.getNumObjectIndex(),
                    GLES20.GL_UNSIGNED_SHORT, myGrid.getIndices());
            GLES20.glDisableVertexAttribArray(vertexHandle);*/
            
            SampleUtils.checkGLError("UserDefinedTargets renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
    }
    
    // inicializa o teapot, carrega as texturas, cria o shaderProgram, o vertexHandle,
    // o textureCoordHandle, o mvpMatrixHandle e o texSampler2DHandle
    private void initRendering()
    {
        Log.d(LOGTAG, "initRendering");
        
        mTeapot = new Teapot();
        myGrid = new MyGrid(20, 200);
        
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
    }
    
    // seta as texturas
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }

    public void increaseSampling(){
        if(myGrid.getLines() < 100)
            myGrid.setN(myGrid.getLines() + 2);
    }

    public void decreaseSampling(){
        if(myGrid.getLines() > 2)
            myGrid.setN(myGrid.getLines() - 2);
    }

    public void increaseSize(){
        if(myGrid.getWidth() < 500)
            myGrid.setSize(myGrid.getWidth() + 20);
    }

    public void decreaseSize(){
        if(myGrid.getWidth() > 20)
            myGrid.setSize(myGrid.getWidth() - 20);
    }

}
