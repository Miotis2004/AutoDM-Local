$ErrorActionPreference = "Stop"
function Invoke-Test($label, $method, $uri) {
    try {
        $resp = Invoke-WebRequest -Uri $uri -Method $method -TimeoutSec 5
        Write-Output "[$label] $method $uri -> $($resp.StatusCode)"
        Write-Output ($resp.Content -replace '\s+', ' ')
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Output "[$label] $method $uri -> HTTP $code"
    }
}

$created = (Invoke-WebRequest -Uri "http://localhost:5150/api/campaign-management" `
    -Method Post -ContentType "application/json" `
    -Body '{"title":"Test Campaign"}' -TimeoutSec 5).Content
$id = ((ConvertFrom-Json $created).id)
Write-Output "Created campaign id=$id"

Invoke-Test "region" POST "http://localhost:5150/api/campaigns/$id/regions?name=Westerlands"
Invoke-Test "loc1" POST "http://localhost:5150/api/campaigns/$id/locations?name=Town&description=Home"
Invoke-Test "loc2" POST "http://localhost:5150/api/campaigns/$id/locations?name=City"
$locs = ((ConvertFrom-Json (Invoke-WebRequest -Uri "http://localhost:5150/api/campaigns/$id/locations" -Method Get -TimeoutSec 5).Content)).id
Write-Output "Location ids: $([string]::join(',',$locs))"
$f = $locs[0]; $t = $locs[1]
Invoke-Test "route" POST "http://localhost:5150/api/campaigns/$id/routes?fromId=$f&toId=$t&distanceKm=40&travelMinutes=90"
Invoke-Test "plan"  GET  "http://localhost:5150/api/campaigns/$id/routes/$f/to/$t/plan"
Invoke-Test "plan-self" GET "http://localhost:5150/api/campaigns/$id/routes/$f/to/$f/plan"
Invoke-Test "party" POST "http://localhost:5150/api/campaigns/$id/party-location?locationId=$f"
Invoke-Test "party-get" GET "http://localhost:5150/api/campaigns/$id/party-location"
Invoke-Test "discover" POST "http://localhost:5150/api/campaigns/$id/locations/$f/discover"
Invoke-Test "discovered-list" GET "http://localhost:5150/api/campaigns/$id/locations/discovered"
Invoke-Test "settlements" POST "http://localhost:5150/api/campaigns/$id/settlements?name=Port&type=TOWN&population=1000"
Invoke-Test "pois" POST "http://localhost:5150/api/campaigns/$id/points-of-interest?name=Tower&category=LANDMARK"
Invoke-Test "listroutes" GET "http://localhost:5150/api/campaigns/$id/routes"
