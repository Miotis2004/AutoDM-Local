$ErrorActionPreference = 'Stop'
$base = 'http://localhost:5150'

# 1. Create a campaign
$camBody = @{ title = 'Test Campaign'; description = 'demo'; status = 'DRAFT'; notes = 'n' } | ConvertTo-Json
$cam = Invoke-RestMethod -Uri "$base/api/campaign-management" -Method Post -Body $camBody -ContentType 'application/json'
Write-Output "Campaign id: $($cam.id)"
$campaignId = $cam.id

# 2. Add an NPC
$npcUrl = "$base/api/campaigns/$campaignId/npcs?name=Garren&description=a+rogue&role=informant&disposition=FRIENDLY&relationship=ALLIED"
$npc = Invoke-RestMethod -Uri $npcUrl -Method Post
Write-Output "NPC id: $($npc.id), name: $($npc.name), disposition: $($npc.disposition), relationship: $($npc.relationship)"

# 3. Update relationship (apply + persist)
$relUrl = "$base/api/campaigns/$campaignId/npcs/$($npc.id)/relationship?relationship=FOE"
$npc2 = Invoke-RestMethod -Uri $relUrl -Method Post
Write-Output "After update, relationship: $($npc2.relationship)"

# 4. List NPCs
$list = Invoke-RestMethod -Uri "$base/api/campaigns/$campaignId/npcs" -Method Get
Write-Output "List count: $($list.Count)"

# 5. Set combat stats
$csUrl = "$base/api/campaigns/$campaignId/npcs/$($npc.id)/combat-stats?maxHitPoints=30&armorClass=15&abilityStrength=16&abilityDexterity=14&abilityConstitution=12&abilityIntelligence=10&abilityWisdom=8&abilityCharisma=12"
$cs = Invoke-RestMethod -Uri $csUrl -Method Post
Write-Output "Combat stats maxHP: $($cs.maxHitPoints), AC: $($cs.armorClass)"

# 6. Verify persistence across 'session' by re-reading from a fresh fetch
$npcReloaded = Invoke-RestMethod -Uri "$base/api/campaigns/$campaignId/npcs/$($npc.id)" -Method Get
Write-Output "Reloaded: name=$($npcReloaded.name) rel=$($npcReloaded.relationship) maxHP=$($npcReloaded.maxHitPoints)"

if ($npcReloaded.relationship -ne 'FOE') { throw "Relationship did not persist" }
if ($npcReloaded.maxHitPoints -ne 30) { throw "Combat stats did not persist" }
Write-Output "ALL_NPC_CHECKS_PASSED"
