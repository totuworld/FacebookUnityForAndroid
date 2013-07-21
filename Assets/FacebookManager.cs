using UnityEngine;
using System.Collections;
using System;

public class FacebookManager : MonoBehaviour {

	private bool _isSessionOpen = false;
	
	private Texture2D _imageTexture = null;
	string resultPrint = "";
	
	//for calculate screen size
	int _safeWidth = 0; 
	int _safeHeight = 0;
	
	void Start () 
	{
		_safeWidth = Screen.width - 20;
		_safeHeight = Screen.height - 190;
    }
	
	void Update () {
	
	}
	
	void OnGUI()
 	{
		// Login button
		if( !_isSessionOpen )
		{
			if (GUI.Button(
				new Rect(10, 10, 200, 100), 
				"Facebook Login"))
			{
				FacebookLogin();
			}
		}
	
		// profile image display after login
		if( _imageTexture != null 
			&& _isSessionOpen == true)
		{
			GUI.DrawTexture(
				new Rect(10, 110, 50, 50), 
				_imageTexture);
		}
		
		// print result or error
		if( resultPrint.Length > 2)
		{
			GUI.Label (
				new Rect (10, 170, _safeWidth, _safeHeight), 
				resultPrint);
		}
	}
	
	void FacebookLogin()
	{
	#if UNITY_ANDROID
		try
		{
			using (AndroidJavaClass jc = 
				new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
			{
				using (AndroidJavaObject jo = 
					jc.GetStatic<AndroidJavaObject>("currentActivity"))
				{
					Debug.Log("facebook login"); 

					jo.Call("FacebookLogin");

				}
			}
		}
		catch (Exception e)
		{
			Debug.Log(e.StackTrace);
		}
	#endif
	}
	
	void didLogin(string id, string fullJson)
	{
		_isSessionOpen = true;
		StartCoroutine(LoadProfileImage(id) );
		StartCoroutine(LoadYourFacebookData(id));
	}
	
	void errorPrint(string error)
	{
		resultPrint = error;
	}
	
	private IEnumerator LoadProfileImage(string id)
    {
       	WWW imageRequest = 
			new WWW("http://graph.facebook.com/"+id+"/picture");
    	yield return imageRequest;
		if( imageRequest.isDone)
		{
			_imageTexture = imageRequest.texture;
		}
    }
	
	private IEnumerator LoadYourFacebookData(string id)
	{
		WWW fbDataRequest = 
			new WWW("http://graph.facebook.com/" + id);
		yield return fbDataRequest;
		if( fbDataRequest.isDone)
		{
			resultPrint = fbDataRequest.text;
		}
	}
	
}
