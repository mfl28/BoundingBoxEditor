$ErrorActionPreference = 'Stop';
$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url64      = 'https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.3.0/boundingboxeditor-2.3.0.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  url64bit      = $url64
  softwareName  = 'boundingboxeditor*'
  checksum64    = '0F78324257400D195A3AB16538B7A0158CE1F4B3266B5C27CEFC6F71B9FD7BBA'
  checksumType64= 'sha256'
  silentArgs    = '/quiet'
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs
