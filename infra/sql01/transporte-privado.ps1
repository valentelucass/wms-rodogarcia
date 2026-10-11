# Biblioteca SQL01; definicoes somente. Nunca importa credencial nem inicia processo ao carregar.
function Initialize-WmsSql01PrivateTransport {
    if ('WmsSql01PrivatePipe' -as [type]) { return }
    Add-Type -ReferencedAssemblies 'System.Web.Extensions' -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Security;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;
public static class WmsSql01PrivatePipe {
    static char[] Characters(SecureString value) {
        IntPtr pointer=Marshal.SecureStringToGlobalAllocUnicode(value);
        try { char[] chars=new char[value.Length];Marshal.Copy(pointer,chars,0,chars.Length);return chars; }
        finally {Marshal.ZeroFreeGlobalAllocUnicode(pointer);}
    }
    static void Integer(Stream stream,int value) {
        stream.WriteByte((byte)(value>>24));stream.WriteByte((byte)(value>>16));
        stream.WriteByte((byte)(value>>8));stream.WriteByte((byte)value);
    }
    static void Frame(Stream stream,char[] chars,int maximum) {
        byte[] bytes=new UTF8Encoding(false,true).GetBytes(chars);
        try {if(bytes.Length<1||bytes.Length>maximum)throw new InvalidOperationException("SQL01_PRIVATE_FRAME_INVALID");
            Integer(stream,bytes.Length);stream.Write(bytes,0,bytes.Length);}
        finally {Array.Clear(bytes,0,bytes.Length);}
    }
    public static void Write(Stream stream,SecureString password,SecureString nativeMaterial) {
        char[] pass=null,json=null,key=null;Dictionary<string,object> material=null;
        try {
            pass=Characters(password);json=Characters(nativeMaterial);
            if(pass.Length<32||pass.Length>1024||json.Length>32768)throw new InvalidOperationException("SQL01_PRIVATE_MATERIAL_INVALID");
            material=new JavaScriptSerializer().Deserialize<Dictionary<string,object>>(new string(json));
            if(!material.ContainsKey("schema")||Convert.ToInt32(material["schema"])!=1||
                !material.ContainsKey("privateKey"))throw new InvalidOperationException("SQL01_PRIVATE_MATERIAL_INVALID");
            key=((string)material["privateKey"]).ToCharArray();
            if(!Regex.IsMatch(new string(key),"^[A-Za-z0-9+/=]+$"))throw new InvalidOperationException("SQL01_PRIVATE_MATERIAL_INVALID");
            Integer(stream,0x574d5301);Frame(stream,pass,4096);Frame(stream,key,16384);Integer(stream,0);stream.Flush();
        } finally {
            foreach(char[] buffer in new[]{pass,json,key})if(buffer!=null)Array.Clear(buffer,0,buffer.Length);
            if(material!=null)material.Clear();
        }
    }
}
'@
}

function Send-WmsSql01PrivateMaterial([IO.Stream]$Stream,[Security.SecureString]$Password,[Security.SecureString]$NativeMaterial) {
    Initialize-WmsSql01PrivateTransport
    [WmsSql01PrivatePipe]::Write($Stream,$Password,$NativeMaterial)
}
