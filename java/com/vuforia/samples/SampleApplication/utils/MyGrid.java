/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.SampleApplication.utils;

import android.util.Log;

import java.nio.Buffer;


public class MyGrid extends MeshObject
{
    
    private Buffer mVertBuff;
    //private Buffer mTexCoordBuff;
    //private Buffer mNormBuff;
    private Buffer mIndBuff;

    private static final String LOGTAG = "MyGrid";
    
    private int indicesNumber = 0;
    private int verticesNumber = 0;

    private int mLines = 0;
    private int mColumns = 0;
    private int mHeight = 0;
    private int mWidth = 0;
    
    
    public MyGrid(int lines, int columns, int width, int height)
    {
        mLines = lines;
        mColumns = columns;
        mHeight = height;
        mWidth = width;
        setVerts();
        setIndices();

    }

    public MyGrid(int n, int size)
    {
        this(n, n, size , size);
    }
    
    // setta os vertices do grid em um bytebuffer a ser passado pro pipeline do openGLES
    private void setVerts()
    {

        int n = mLines+ mColumns+ 2;
        float[] VERTS = new float[6 * n];
        float step = mWidth / mLines;
        float beginY = mWidth/2;
        float beginX = -mWidth/2;
        float endY = -mHeight/2;
        float endX = mHeight/2;

        for(int i=0; i <= mLines; ++i){
            VERTS[6*i] = beginX;
            VERTS[6*i+1] = beginY - i*step;
            VERTS[6*i+2] = 0;
            VERTS[6*i+3] = endX;
            VERTS[6*i+4] = beginY - i*step;
            VERTS[6*i+5] = 0;
        }
        Log.d(LOGTAG, "Successfully created"+(mLines+1)+" lines");

        step = mHeight / mColumns;

        for(int i=mLines+1; i < n; ++i){
            VERTS[6*i] = beginX + (i-mLines-1)*step;
            VERTS[6*i+1] = beginY;
            VERTS[6*i+2] = 0;
            VERTS[6*i+3] = beginX + (i-mLines-1)*step;
            VERTS[6*i+4] = endY;
            VERTS[6*i+5] = 0;
        }
        Log.d(LOGTAG, "Successfully created"+(n-(mLines+1))+" collumns");

        // preenche o bytebuffer com os vertices
        mVertBuff = fillBuffer(VERTS);
        // setta o numero de vertices do grid
        verticesNumber = VERTS.length / 3;
    }

    private void setIndices()
    {
        int n = mLines + mColumns + 2;
        short[] INDICES = new short[2 * n];

        for(int i=0; i <= mLines; ++i){
            INDICES[2*i] = (short)(2 * i);
            INDICES[2*i+1] = (short)(2 * i + 1);
        }

        for(int i=mLines+1; i < n; ++i){
            INDICES[2*i] = (short)(2 * i);
            INDICES[2*i+1] = (short)(2 * i + 1);
        }
        mIndBuff = fillBuffer(INDICES);
        indicesNumber = INDICES.length;
    }

    public int getLines(){
        return mLines;
    }

    public int getColumns(){
        return mColumns;
    }

    public int getWidth(){
        return mWidth;
    }

    public int getHeight(){
        return mHeight;
    }
    
    public void setLines(int lines){
        mLines = lines;
        setVerts();
        setIndices();
    }

    public void setColumns(int columns){
        mColumns= columns;
        setVerts();
        setIndices();
    }

    public void setN(int n){
        mLines = n;
        mColumns= n;
        setVerts();
        setIndices();
    }

    public void setSize(int size){
        mWidth = size;
        mHeight = size;
        setVerts();
        setIndices();
    }

    public void setWidth(int width){
        mWidth = width;
        setVerts();
        setIndices();
    }

    public void setHeight(int height){
        mHeight = height;
        setVerts();
        setIndices();
    }

    public int getNumObjectIndex()
    {
        return indicesNumber;
    }


    // retona o numero de vertices do teapot
    @Override
    public int getNumObjectVertex()
    {
        return verticesNumber;
    }
    
    // retorna o bytebuffer de acordo com o tipo passado como parametro
    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_INDICES:
                result = mIndBuff;
            default:
                break;
        
        }
        
        return result;
    }
    
}
