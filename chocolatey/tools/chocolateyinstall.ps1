$ErrorActionPreference = 'Stop';
$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url64      = 'https://github.com/mfl28/BoundingBoxEditor/releases/download/v2.5.1/boundingboxeditor-2.5.1.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  url64bit      = $url64
  softwareName  = 'boundingboxeditor*'
  checksum64    = 'CF59E2E162D820BC29EBC720EABC01E9630B1B4FE6E22334DB27F8C4FA6FAD50'
  checksumType64= 'sha256'
  silentArgs    = '/quiet'
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs
