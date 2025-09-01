using UnityEngine;
using System.Collections;
using System.Linq;
using UnityEngine.Windows.WebCam;

using System;
using System.IO;
using XRCloudServices;

using System.Collections.Generic;
using System.IO;
using UnityEngine.UI;
using UnityEngine.Networking;
using TMPro;
using Microsoft.MixedReality.Toolkit.Audio;


public class TakePicAndProcessAI : MonoBehaviour
{
    public Material quadMaterial = null;
    UnityEngine.Windows.WebCam.PhotoCapture photoCaptureObject = null;
    Texture2D targetTexture = null;
    int tapsCount = 1;
    string filenameFromStorage;
    string filePath;

    public string backendURL = "https://yourhost:yourport";
    public GameObject gameojbectToActivate;
    public GameObject textToSpeechObject;
    public TextToSpeechLogic textToSpeechLogic;

    [SerializeField] private TMP_Text textMesh;
    GameObject quad;

    private bool isInit = false;

    private string endPoint;

    private string genopts;

    public void Start()
    {
       TakePicAndProcess();
    }
    public void ExplainThis()
    {
        endPoint = "visionai/explain";
       TakePicAndProcess();
    }
    public void SummarizeThis()
    {
        endPoint = "visionai/summarize";
        TakePicAndProcess();
    }
    public void Transcribe()
    {
        endPoint = "visionai/transcribe";
        TakePicAndProcess();
    }
    public void TellAScaryStoryAndSentiments()
    {
        endPoint = "tellastory/tellastory";
        genopts = "scary";
        TakePicAndProcess();
    }
    public void TellADystopianStoryAndSentiments()
    {
        endPoint = "tellastory/tellastory";
        genopts = "dystopian";
        TakePicAndProcess();
    }
    public void GenerateAnImageFromDescription()
    {
        endPoint = "tellastory/tellastory";
        genopts = "scary";
        TakePicAndProcess();
    }

    private void TakePicAndProcess()
    {
        Resolution cameraResolution = UnityEngine.Windows.WebCam.PhotoCapture.SupportedResolutions.OrderByDescending((res) => res.width * res.height).First();
        Texture2D targetTexture = new Texture2D(cameraResolution.width, cameraResolution.height);
        UnityEngine.Windows.WebCam.PhotoCapture.CreateAsync(false, delegate (UnityEngine.Windows.WebCam.PhotoCapture captureObject)
        {
            photoCaptureObject = captureObject;
            CameraParameters camParameters = new CameraParameters();
            camParameters.hologramOpacity = 0.0f;
            camParameters.cameraResolutionWidth = targetTexture.width;
            camParameters.cameraResolutionHeight = targetTexture.height;
            camParameters.pixelFormat = CapturePixelFormat.BGRA32; //other options include JPEG, NV12, etc.;
            captureObject.StartPhotoModeAsync(camParameters, delegate (UnityEngine.Windows.WebCam.PhotoCapture.PhotoCaptureResult result)
             {
                 filenameFromStorage = string.Format(@"CapturedImage{0}.jpg", tapsCount++);
                 filePath = Path.Combine(Application.persistentDataPath, filenameFromStorage);
                 if (textMesh != null) textMesh.text = "pic file:" + filePath;
                 photoCaptureObject.TakePhotoAsync(filePath, PhotoCaptureFileOutputFormat.JPG, OnCapturedPhotoToDisk);
             });
        });
    }
    void OnCapturedPhotoToDisk(UnityEngine.Windows.WebCam.PhotoCapture.PhotoCaptureResult result)
    {
        if (!isInit)
        {
            photoCaptureObject.StopPhotoModeAsync(OnStoppedPhotoMode);
            isInit = true;
            return;
        }
        StartCoroutine(ProcessAI(filePath));
        Texture2D tex = new Texture2D(2, 2);
        tex.LoadImage(File.ReadAllBytes(filePath));

        GameObject quad = GameObject.CreatePrimitive(PrimitiveType.Quad);
        quad.transform.SetParent(Camera.main.transform);
        quad.transform.SetParent(gameojbectToActivate.transform);

        quad.transform.localPosition = new Vector3(0, 0, 2); // Adjust this to position the quad correctly
        quad.transform.localScale = new Vector3(0.2f, 0.2f, 0.2f); // Adjust this to scale the quad correctly

        quad.transform.localEulerAngles = Vector3.zero;  // Rotate it to face the camera
        quad.GetComponent<Renderer>().material.mainTexture = tex;


        photoCaptureObject.StopPhotoModeAsync(OnStoppedPhotoMode);
    }

    private IEnumerator ProcessAI(string filePath)
    {
        Texture2D texture2D = new Texture2D(2, 2);
        byte[] imageData = File.ReadAllBytes(filePath);
        texture2D.LoadImage(imageData);
        yield return ProcessAI(filePath, texture2D);
    }

    private IEnumerator ProcessAI(string filePath, Texture2D image)
    {
        if (genopts != null)
        {
            if (textMesh != null) textMesh.text = generatedStory;
            textToSpeechLogic.talk(textToSpeechObject.GetComponent<TextToSpeech>(), generatedStory);
			yield return null;
        }
        if (textMesh != null) textMesh.text = "in " + endPoint +" making call to Vision AI + GPT for file: " + filePath + "...";
        byte[] imageData = image.EncodeToPNG();
        WWWForm form = new WWWForm();
        form.AddBinaryData("file", imageData, "image.png", "image/png");
        if (genopts != null)
        {
            form.AddField("genopts", genopts);
            genopts = null;
        }
        Debug.Log("Making AI etc. calls, providing file: " + filePath + "...");
        if (textMesh != null)
        {
            textMesh.text = "in " + endPoint +" about to textToSpeechObject: " + textToSpeechObject;
            textToSpeechObject.SetActive(true);
            if (textMesh != null) textMesh.text = "in " + endPoint +" about to gameojbectToActivate and call server: " + gameojbectToActivate;
        }
        UnityWebRequest request = UnityWebRequest.Post(backendURL + "/" + endPoint, form);
        yield return request.SendWebRequest();
        if (request.result == UnityWebRequest.Result.Success)
        {
            string jsonResponse = request.downloadHandler.text;
            if (textMesh != null) textMesh.text = jsonResponse;
            textToSpeechLogic.talk(textToSpeechObject.GetComponent<TextToSpeech>(), jsonResponse);
            Debug.Log(jsonResponse);
        }
        else
        {
            if (textMesh != null) textMesh.text = "request.error=" + request.error;
            textToSpeechLogic.talk(textToSpeechObject.GetComponent<TextToSpeech>(), request.error);
            Debug.Log(request.error);
        }
        request.Dispose();
    }

    void OnStoppedPhotoMode(UnityEngine.Windows.WebCam.PhotoCapture.PhotoCaptureResult result)
    {
        photoCaptureObject.Dispose();
        photoCaptureObject = null;
    }

    public void Clear()
    {
        quad.SetActive(false);
        quad = null;
        gameojbectToActivate.SetActive(false);
        gameojbectToActivate = null;
    }
}
