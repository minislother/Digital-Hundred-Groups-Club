param(
    [string]$EsUrl = "http://localhost:9200",
    [string]$Index = "baituan_activity_v2"
)

$mapping = Get-Content "$PSScriptRoot/../../club_springboot/src/main/resources/search/activity-index.json" -Raw | ConvertFrom-Json -AsHashtable
$mapping['aliases'] = @{ $Index = @{} }
$exists = Invoke-WebRequest "$EsUrl/$Index" -Method Head -SkipHttpErrorCheck
if ($exists.StatusCode -eq 200) {
    Write-Host "Index already exists: $Index"
    exit 0
}

Invoke-RestMethod "$EsUrl/$($Index)_000001" -Method Put -ContentType "application/json" -Body ($mapping | ConvertTo-Json -Depth 20)
Write-Host "Created index: $Index"
