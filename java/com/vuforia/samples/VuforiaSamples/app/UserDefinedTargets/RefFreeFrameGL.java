/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.UserDefinedTargets;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.res.Configuration;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.Vec4F;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Texture;


public class RefFreeFrameGL
{
    
    private static final String LOGTAG = "RefFreeFrameGL";
    
    UserDefinedTargets mActivity;
    SampleApplicationSession vuforiaAppSession;

    private final class TEXTURE_NAME
    {
        // Viewfinder in portrait mode
        public final static int TEXTURE_VIEWFINDER_MARKS_PORTRAIT = 0; 
        
        // Viewfinder in landscape mode
        public final static int TEXTURE_VIEWFINDER_MARKS = 1; 
        
        // Not a texture, count of predef textures
        public final static int TEXTURE_COUNT = 2; 
    }
    
    // OpenGL handles for the various shader related variables
    private int shaderProgramID; // The Shaders themselves
    private int vertexHandle; // Handle to the Vertex Array
    private int textureCoordHandle; // Handle to the Texture Coord Array
    private int colorHandle; // Handle to the color vector
    private int mvpMatrixHandle; // Handle to the product of the Projection
                                 // and Modelview Matrices
    private int texSampler2DHandle; // Handle to the texture for the viewfinder status
    
    // Projection and Modelview Matrices
    //Matrix44F projectionOrtho, modelview;
    float[] projectionOrtho, modelview;
    
    // Color vector
    Vec4F color;
    
    // Texture names and textures
    // nomes das texturas (ainda nao vi essas texturas sendo usadas durante a execucao do app)
    String textureNames[] = {
            "UserDefinedTargets/viewfinder_crop_marks_portrait.png",
            "UserDefinedTargets/viewfinder_crop_marks_landscape.png" };
    Texture[] textures;
    
    // Vertices, texture coordinates and vector indices
    int NUM_FRAME_VERTEX_TOTAL = 4;
    //int NUM_FRAME_INDEX = 1 + NUM_FRAME_VERTEX_TOTAL;
    int NUM_FRAME_INDEX = NUM_FRAME_VERTEX_TOTAL;
    
    float frameVertices_viewfinder[] = new float[NUM_FRAME_VERTEX_TOTAL * 3];
    float frameTexCoords[] = new float[NUM_FRAME_VERTEX_TOTAL * 2];
    short frameIndices[] = new short[NUM_FRAME_INDEX];
    
    // Portrait/Landscape status detected in init()
    boolean isActivityPortrait;

    // codigo em shader necessario para renderizar os vertices de objetos
    String frameVertexShader = " \n" + "attribute vec4 vertexPosition; \n"
        + "attribute vec2 vertexTexCoord; \n" + "\n"
        + "varying vec2 texCoord; \n" + "\n"
        + "uniform mat4 modelViewProjectionMatrix; \n" + "\n"
        + "void main() \n" + "{ \n"
        + "gl_Position = modelViewProjectionMatrix * vertexPosition; \n"
        + "texCoord = vertexTexCoord; \n" + "} \n";

    // codigo em shader necessario para renderizar as cores e texturas de objetos
    String frameFragmentShader = " \n" + "precision mediump float; \n" + "\n"
        + "varying vec2 texCoord; \n" + "\n"
        + "uniform sampler2D texSampler2D; \n" + "uniform vec4 keyColor; \n"
        + "\n" + "void main() \n" + "{ \n"
        + "vec4 texColor = texture2D(texSampler2D, texCoord); \n"
        + "gl_FragColor = keyColor * texColor; \n" + "} \n" + "";
    
    // construtor que toma como parametros e salva referencias para instancias de atividade
    // e session do vuforia, e inicializa algumas propriedade desta classe
    public RefFreeFrameGL(UserDefinedTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
        shaderProgramID = 0;
        vertexHandle = 0;
        textureCoordHandle = 0;
        mvpMatrixHandle = 0;
        texSampler2DHandle = 0;
        
        Log.d(LOGTAG, "RefFreeFrameGL Ctor");
        textures = new Texture[TEXTURE_NAME.TEXTURE_COUNT];
        for (int i = 0; i < TEXTURE_NAME.TEXTURE_COUNT; i++)
            textures[i] = null;
        
        color = new Vec4F();
    }
    
    
    // Quickly set the color for rendering
    void setColor(float r, float g, float b, float a)
    {
        float[] tempColor = { r, g, b, a };
        color.setData(tempColor);
    }
    
    // setta o vetor de cor
    void setColor(float c[])
    {
        if (c.length != 4)
            throw new IllegalArgumentException(
                "Color length must be 4 floats length");
        
        color.setData(c);
    }
    
    // Set the scale for the model view matrix
    // nao entendi pq o fator de escala eh settado para a posicao 14 da matriz
    void setModelViewScale(float scale)
    {
        /*float[] tempModelViewData = modelview.getData();
        tempModelViewData[14] = scale;
        modelview.setData(tempModelViewData);*/
        modelview[14] = scale;
    }
    
    // inicializa as coisas do opengl necessarias para renderizar a cena, como modelview settada
    // na matriz identidade, cor como branco puro, orientacao do device, cria os shaders e o
    // OpenGL ES object, inicializa a Orthograpic projection matrix para settar os verticese
    // vertices de textura do viewfinder (ainda nao entendi no que esse viewfinder eh usado)
    boolean init(int screenWidth, int screenHeight)
    {
       /* float tempMatrix44Array[] = new float[16];
        // modelview matrix set to identity
        modelview = new Matrix44F();
        
        tempMatrix44Array[0] = tempMatrix44Array[5] = tempMatrix44Array[10] = tempMatrix44Array[15] = 1.0f;
        modelview.setData(tempMatrix44Array);*/
        
        // color is set to pure white
        float tempColor[] = { 1.0f, 1.0f, 1.0f, 0.6f };
        color.setData(tempColor);
        
        // Detect if we are in portrait mode or not
        Configuration config = mActivity.getResources().getConfiguration();
        
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE)
            isActivityPortrait = false;
        else
            isActivityPortrait = true;

        // cria um OpenGL ES object que contem os shaders e sera usado para desenhar as formas na
        // tela
        if ((shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            frameVertexShader, frameFragmentShader)) == 0)
            return false;
        //pega a referencia pra vaiavel vertexPosition declarada la no codigo do vertexshader
        if ((vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition")) == -1)
            return false;
        //pega a referencia pra variavel vertexTexCoord declarada la no codigo do vertexshader e
        // no codigo do fragmentshader (ela eh compartilhada pelos dois shaders)
        if ((textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord")) == -1)
            return false;
        //pega a referencia pra vaiavel modelViewProjectionMatrix declarada la no codigo do
        // vertexshader
        if ((mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix")) == -1)
            return false;
        //pega a referencia pra vaiavel keyColor declarada la no codigo do fragmentshader
        if ((colorHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "keyColor")) == -1)
            return false;
        //pega a referencia pra vaiavel texSampler2D declarada la no codigo do fragmentshader
        if ((texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D")) == -1)
            return false;
        
        // retrieves the screen size and other video background config values
        Renderer renderer = Renderer.getInstance();
        VideoBackgroundConfig vc = renderer.getVideoBackgroundConfig();
        
        // makes ortho matrix
        /*projectionOrtho = new Matrix44F();
        for (int i = 0; i < tempMatrix44Array.length; i++)
        {
            tempMatrix44Array[i] = 0;
        }
        
        int tempVC[] = vc.getSize().getData();
        
        // Calculate the Orthograpic projection matrix
        tempMatrix44Array[0] = 2.0f / (float) (tempVC[0]);
        tempMatrix44Array[5] = 2.0f / (float) (tempVC[1]);
        tempMatrix44Array[10] = 1.0f / (-10.0f);
        tempMatrix44Array[11] = -5.0f / (-10.0f);
        tempMatrix44Array[15] = 1.0f;

        
        // Viewfinder size based on the Ortho matrix because it is an Ortho UI
        // element use the ratio of the reported screen size and the calculated
        // screen size to account for on screen OS UI elements such as the 
        // action bar in ICS.
        float sizeH_viewfinder = ((float) screenWidth / tempVC[0])
            * (2.0f / tempMatrix44Array[0]);
        float sizeV_viewfinder = ((float) screenHeight / tempVC[1])
            * (2.0f / tempMatrix44Array[5]);
        
        Log.d(LOGTAG, "Viewfinder Size: " + sizeH_viewfinder + ", "
            + sizeV_viewfinder);*/

        projectionOrtho = new float[16];
        // define a minha projecao ortografica
        Matrix.orthoM(projectionOrtho, 0, -0.5f, 0.5f, -0.5f, 0.5f, -1.0f, 1.0f);

        // modelview matrix set to identity
        modelview = new float[16];
        Matrix.setIdentityM(modelview, 0);

        // ** initialize the frame with the correct scale to fit the current
        // perspective matrix
        int cnt = 0, tCnt = 0;
        
        // Define the vertices and texture coords for a triangle strip that will
        // define the Quad where the viewfinder is rendered.
        //
        // 0---------1 | | | | 3---------2
        
        // / Vertex 0
        // vertice superior esquerdo
        frameVertices_viewfinder[cnt++] = -0.5f;//(-1.0f) * sizeH_viewfinder;
        frameVertices_viewfinder[cnt++] =  0.5f;//(1.0f) * sizeV_viewfinder;
        frameVertices_viewfinder[cnt++] =  0.0f;
        frameTexCoords[tCnt++] = 0.0f;
        frameTexCoords[tCnt++] = 0.0f;//1.0f;
        
        // / Vertex 1
        // vertice superior direito
        frameVertices_viewfinder[cnt++] = -0.5f;//(1.0f) * sizeH_viewfinder;
        frameVertices_viewfinder[cnt++] = -0.5f;//(1.0f) * sizeV_viewfinder;
        frameVertices_viewfinder[cnt++] =  0.0f;
        frameTexCoords[tCnt++] = 0.0f;//1.0f;
        frameTexCoords[tCnt++] = 1.0f;
        
        // / Vertex 2
        // vertice inferior direito
        frameVertices_viewfinder[cnt++] = 0.5f;//(1.0f) * sizeH_viewfinder;
        frameVertices_viewfinder[cnt++] = -0.5f;//(-1.0f) * sizeV_viewfinder;
        frameVertices_viewfinder[cnt++] = 0.0f;
        frameTexCoords[tCnt++] = 1.0f;
        frameTexCoords[tCnt++] = 1.0f;//0.0f;
        
        // / Vertex 3
        // vertice inferior esquerdo
        frameVertices_viewfinder[cnt++] = 0.5f;//(-1.0f) * sizeH_viewfinder;
        frameVertices_viewfinder[cnt++] = 0.5f;//(-1.0f) * sizeV_viewfinder;
        frameVertices_viewfinder[cnt++] = 0.0f;
        frameTexCoords[tCnt++] = 1.0f;//0.0f;
        frameTexCoords[tCnt++] = 0.0f;
        
        // we also set the indices programmatically
        cnt = 0;
        for (short i = 0; i < NUM_FRAME_VERTEX_TOTAL; i++)
            frameIndices[cnt++] = i; // one full loop
        //frameIndices[cnt++] = 0; // close the loop
        
        // loads the texture
        for (Texture t : textures)
        {
            if (t != null)
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
        }
        
        return true;
    }

    // aloca o buffer, insere os doubles (como floats) nele e deixa ele pronto pra ser usado (rewind)
    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
                                                                     // float
                                                                     // takes 4
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        
        return bb;
    }

    // aloca o buffer, insere os shorts nele e deixa ele pronto pra ser usado (rewind)
    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
                                                                     // short
                                                                     // takes 2
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        
        return bb;
        
    }
    
    // carrega as texturas do viewfinder
    void getTextures()
    {
        for (int i = 0; i < TEXTURE_NAME.TEXTURE_COUNT; i++)
            textures[i] = mActivity.createTexture(textureNames[i]);
    }
    
    
    // / Renders the viewfinder
    void renderViewfinder()
    {
        if (textures == null)
            return;
        
        // Set GL flags
        //ativa o blending e o aplica
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // desativa o teste de profundidade e o cull_face
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        
        // Set the shader program
        // setta o shadder program pro ambiente opengles
        GLES20.glUseProgram(shaderProgramID);
        
        // Calculate the Projection * ModelView matrix and pass to shader
        float[] mvp = new float[16];
        /*Matrix.multiplyMM(mvp, 0, projectionOrtho.getData(), 0,
            modelview.getData(), 0);*/

        // multiplica as matrizes, dado matriz resultado offset da mesma, primeira matriz e seu
        // offset e a segunda matriz e seu offset
        Matrix.multiplyMM(mvp, 0, projectionOrtho, 0, modelview, 0);
        // setta o valor da variavel uniforme modelViewProjectionMatrix (que ta referenciada pra
        // variavel mvpMatrixHandle) definida la no codigo do shadder  pro valor da variavel mvp
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvp, 0);

        // Set the vertex handle
        // passa os vertices do viewfinder pra um buffer
        Buffer verticesBuffer = fillBuffer(frameVertices_viewfinder);
        // setta o valor do atributo vertexPosition (que ta referenciado pra variavel
        // vertexHandle) definida la no codigo do shadder  pro valor da variavel verticesBuffer
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        
        // Set the Texture coordinate handle
        // passa as coordenadas de textura pra um buffer
        Buffer texCoordBuffer = fillBuffer(frameTexCoords);
        // setta o valor do atributo vertexTexCoord (que ta referenciado pra variavel
        // textureCoordHandle) definida la no codigo do shadder  pro valor da variavel texCoordBuffer
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        
        // Enable the Vertex and Texture arrays
        // necessario ativar essas arrays para poder utiliza-las durante a chamada de
        // glDrawElements
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        
        // Send the color value to the shader
        // setta o valor do atributo keyColor (que ta referenciado pra variavel
        // colorHandle) definida la no codigo do shadder  pro valor da variavel color (colorFrame
        // da classe RefFreeFrame)
        GLES20.glUniform4fv(colorHandle, 1, color.getData(), 0);
        
        // Depending on if we are in portrait or landsacape mode,
        // choose the proper viewfinder texture
        if (isActivityPortrait && textures[TEXTURE_NAME.TEXTURE_VIEWFINDER_MARKS_PORTRAIT] != null)
        {
            // ativa a unidade de textura
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // atrela a textura na unidade ativa
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEXTURE_NAME.TEXTURE_VIEWFINDER_MARKS_PORTRAIT].mTextureID[0]);

        } else if (!isActivityPortrait && textures[TEXTURE_NAME.TEXTURE_VIEWFINDER_MARKS] != null)
        {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEXTURE_NAME.TEXTURE_VIEWFINDER_MARKS].mTextureID[0]);
        }

        // send the viewfinder texture to the shader
        // setta o valor do atributo texSampler2D (que ta referenciado pra variavel
        // texSampler2DHandle) definida la no codigo do shadder  pro valor 0
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Draw the viewfinder
        // passa os indices pra um buffer
        Buffer indicesBuffer = fillBuffer(frameIndices);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, /*NUM_FRAME_INDEX*/frameIndices.length, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
    }
    
}
