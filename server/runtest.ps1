$ErrorActionPreference = "Stop"
Set-Location C:\AgentTests\AutoDM\server
$proc = Start-Process java -ArgumentList "-jar","target\autodm-server-0.0.1-SNAPSHOT.jar" `
    -PassThru -RedirectStandardOutput C:\AgentTests\AutoDM\server\server_out.txt -RedirectStandardError C:\AgentTests\AutoDM\server\server_err.txt
Write-Output "Started PID $($proc.Id)"
for ($i = 0; $i -lt 90; $i++) {
    try {
        Invoke-WebRequest -Uri "http://localhost:5150/api/campaign-management" -Method Get -TimeoutSec 2 | Out-Null
        Write-Output "Server up after $($i)s"
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

function Invoke-Test($method, $uri) {
    try {
        $resp = Invoke-WebRequest -Uri $uri -Method $method -TimeoutSec 5
        Write-Output "$method $uri -> $($resp.StatusCode)"
        Write-Output $resp.Content
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Output "$method $uri -> HTTP $code : $($_.Exception.Message)"
    }
}

# Create a campaign and read its id
$created = (Invoke-WebRequest -Uri "http://localhost:5150/api/campaign-management" `
    -Method Post -ContentType "application/json" `
    -Body '{"name":"Test Campaign"}' -TimeoutSec 5).Content
$id = ((ConvertFrom-Json $created).id)
Write-Output "Created campaign id=$id"

Invoke-Test POST "http://localhost:5150/api/campaigns/$id/regions?name=TestRegion"
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/locations?name=Town"
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/locations?name=City"
$locs = ((ConvertFrom-Json (Invoke-WebRequest -Uri "http://localhost:5150/api/campaigns/$id/locations" -Method Get -TimeoutSec 5).Content)).id
Write-Output "Location ids: $([string]::join(',',$locs))"
$f = $locs[0]; $t = $locs[1]
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/routes?fromId=$f&toId=$t&travelMinutes=30"
Invoke-Test GET  "http://localhost:5150/api/campaigns/$id/routes/$f/to/$t/plan"
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/party-location?locationId=$f"
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/party-location?locationId=1"
Invoke-Test GET  "http://localhost:5150/api/campaigns/$id/party-location"
Invoke-Test POST "http://localhost:5150/api/campaigns/$id/locations/1/discover"
Invoke-Test GET  "http://localhost:5150/api/campaigns/$id/locations/discovered"

Stop-Process -Id $proc.Id -Force
Write-Output "Stopped"
