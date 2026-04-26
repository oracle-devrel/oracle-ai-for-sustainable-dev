package main

import (
	"context"
	"encoding/json"
	"fmt"
	"iter"
	"log"
	"os"
	"regexp"
	"sort"
	"strings"
	"time"

	"google.golang.org/adk/agent"
	"google.golang.org/adk/agent/llmagent"
	"google.golang.org/adk/agent/workflowagents/parallelagent"
	"google.golang.org/adk/agent/workflowagents/sequentialagent"
	"google.golang.org/adk/model/gemini"
	"google.golang.org/adk/session"
	"google.golang.org/adk/tool"
	"google.golang.org/adk/tool/functiontool"
	"google.golang.org/genai"
)

const (
	appName          = "oracle-inventory-action-go"
	defaultModelName = "gemini-2.0-flash"
	defaultProductID = "SKU-500"
)

var productIDPattern = regexp.MustCompile(`\b([A-Z]{2,}-\d+)\b`)

type InventoryActionResult struct {
	ResponseText      string                 `json:"responseText"`
	Trace             []string               `json:"trace"`
	OrchestrationMode string                 `json:"orchestrationMode"`
	DraftAction       map[string]any         `json:"draftAction,omitempty"`
	PolicyResult      map[string]any         `json:"policyResult,omitempty"`
}

type GraphEvidenceArgs struct {
	ProductID string `json:"productId" jsonschema_description:"The product identifier, such as SKU-500."`
}

type GraphEvidenceResult struct {
	Status         string `json:"status"`
	ProductID      string `json:"productId"`
	DependencyPath string `json:"dependencyPath,omitempty"`
	Warehouse      string `json:"warehouse,omitempty"`
	WarehouseMetric string `json:"warehouseMetric,omitempty"`
	ActiveAlert    string `json:"activeAlert,omitempty"`
	AlertDetail    string `json:"alertDetail,omitempty"`
	AlertMetric    string `json:"alertMetric,omitempty"`
	SourceMode     string `json:"sourceMode,omitempty"`
	SourceDetail   string `json:"sourceDetail,omitempty"`
	Message        string `json:"message,omitempty"`
}

type SpatialEvidenceArgs struct {
	ProductID string `json:"productId" jsonschema_description:"The product identifier, such as SKU-500."`
}

type SpatialEvidenceResult struct {
	Status                        string  `json:"status"`
	ProductID                     string  `json:"productId"`
	HotspotRegion                 string  `json:"hotspotRegion"`
	HotspotSummary                string  `json:"hotspotSummary"`
	RecommendedSourceWarehouse    string  `json:"recommendedSourceWarehouse"`
	RecommendedDestinationWarehouse string `json:"recommendedDestinationWarehouse"`
	SuggestedTransferUnits        int     `json:"suggestedTransferUnits"`
	CoverageRiskDays              float64 `json:"coverageRiskDays"`
	SourceDetail                  string  `json:"sourceDetail"`
}

type ExternalSignalsArgs struct {
	ProductID string `json:"productId" jsonschema_description:"The product identifier, such as SKU-500."`
}

type ExternalSignalsResult struct {
	Status                   string `json:"status"`
	ProductID                string `json:"productId"`
	SignalName               string `json:"signalName"`
	SignalSummary            string `json:"signalSummary"`
	RiskLevel                string `json:"riskLevel"`
	RecommendedLeadTimeAction string `json:"recommendedLeadTimeAction"`
	ObservedDate             string `json:"observedDate"`
	SourceDetail             string `json:"sourceDetail"`
}

type TransferPolicyArgs struct {
	ProductID            string `json:"productId"`
	SourceWarehouse      string `json:"sourceWarehouse"`
	DestinationWarehouse string `json:"destinationWarehouse"`
	Units                int    `json:"units"`
	Reason               string `json:"reason"`
}

type TransferPolicyResult struct {
	Status                    string `json:"status"`
	ProductID                 string `json:"productId"`
	Allowed                   bool   `json:"allowed"`
	RequiresApproval          bool   `json:"requiresApproval"`
	PolicySummary             string `json:"policySummary"`
	UnitsThresholdForApproval int    `json:"unitsThresholdForApproval"`
	Reason                    string `json:"reason"`
}

type DraftInventoryTransferArgs struct {
	ProductID            string `json:"productId"`
	SourceWarehouse      string `json:"sourceWarehouse"`
	DestinationWarehouse string `json:"destinationWarehouse"`
	Units                int    `json:"units"`
	Reason               string `json:"reason"`
}

type DraftInventoryTransferResult struct {
	Status             string `json:"status"`
	ActionType         string `json:"actionType"`
	DraftActionID      string `json:"draftActionId"`
	ProductID          string `json:"productId"`
	SourceWarehouse    string `json:"sourceWarehouse"`
	DestinationWarehouse string `json:"destinationWarehouse"`
	Units              int    `json:"units"`
	Reason             string `json:"reason"`
	ExecutionState     string `json:"executionState"`
	ApprovalState      string `json:"approvalState"`
}

type graphSnapshot struct {
	SupplierName    string
	TierLevel       string
	SupplierRegion  string
	OnTimePct       string
	PlantName       string
	CycleDays       string
	UtilizationPct  string
	PortName        string
	ETAHours        string
	DelayRiskScore  string
	WarehouseName   string
	InventoryUnits  string
	FillRatePct     string
	ProductID       string
	DemandChangePct string
	MarginPct       string
	AlertName       string
	LaneName        string
	AlertRisk       string
}

type inventoryRiskSummary struct {
	ProductID                 string
	ProductName               string
	QuarterLabel              string
	RiskLevel                 string
	StockoutProbability       float64
	ProjectedRevenueImpactUSD float64
	PrimaryRegion             string
	RecommendationSummary     string
}

type warehouseHotspot struct {
	ProductID       string
	WarehouseCode   string
	WarehouseName   string
	CountyName      string
	StateCode       string
	RegionName      string
	HotspotRank     int
	HotspotScore    float64
	CoverageDays    float64
	BacklogUnits    int
	ServiceLevelPct float64
	AtRiskUnits     int
	RevenueImpactUSD float64
	RiskLevel       string
	RecommendedRole string
}

type externalSignal struct {
	SignalName               string
	SignalSummary            string
	RiskLevel                string
	RecommendedLeadTimeAction string
}

type inventoryActionTools struct{}

type inventoryActionCoordinator struct {
	env map[string]string
}

type deterministicAgent struct {
	name        string
	description string
	coordinator *inventoryActionCoordinator
}

func newInventoryActionCoordinator() *inventoryActionCoordinator {
	return &inventoryActionCoordinator{env: currentEnv()}
}

func currentEnv() map[string]string {
	env := make(map[string]string)
	for _, pair := range os.Environ() {
		parts := strings.SplitN(pair, "=", 2)
		if len(parts) == 2 {
			env[parts[0]] = parts[1]
		}
	}
	return env
}

func normalizeProductID(productID string) string {
	normalized := strings.TrimSpace(strings.ToUpper(productID))
	if normalized == "" {
		return defaultProductID
	}
	return normalized
}

func containsProductID(userInput string) bool {
	return productIDPattern.MatchString(strings.ToUpper(userInput))
}

func extractProductID(userInput string) string {
	match := productIDPattern.FindStringSubmatch(strings.ToUpper(userInput))
	if len(match) > 1 {
		return match[1]
	}
	return defaultProductID
}

func valueOrDefault(value string, fallback string) string {
	if strings.TrimSpace(value) == "" {
		return fallback
	}
	return strings.TrimSpace(value)
}

func toSentence(value string) string {
	normalized := strings.TrimSpace(value)
	if normalized == "" {
		return ""
	}
	normalized = strings.TrimRight(normalized, ".!? ")
	return normalized + "."
}

func toParenthetical(value string) string {
	normalized := strings.TrimSpace(value)
	if normalized == "" {
		return "unknown error"
	}
	return strings.TrimRight(normalized, ".!? ")
}

func formatPercent(value string) string {
	return valueOrDefault(value, "n/a") + "%"
}

func formatSignedPercent(value string) string {
	normalized := strings.TrimSpace(value)
	if normalized == "" {
		return "n/a"
	}
	if strings.HasPrefix(normalized, "-") {
		return normalized + "%"
	}
	return "+" + normalized + "%"
}

func combinedReason(activeAlert string, signalSummary string) string {
	parts := make([]string, 0, 2)
	if strings.TrimSpace(activeAlert) != "" {
		parts = append(parts, "Graph: "+toParenthetical(activeAlert)+".")
	}
	if strings.TrimSpace(signalSummary) != "" {
		parts = append(parts, "External: "+toParenthetical(signalSummary)+".")
	}
	if len(parts) == 0 {
		return "Inventory risk evidence supports a balancing transfer."
	}
	return strings.Join(parts, " ")
}

func firstNonBlank(values ...string) string {
	for _, value := range values {
		if strings.TrimSpace(value) != "" {
			return strings.TrimSpace(value)
		}
	}
	return ""
}

func truthy(value string) bool {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "1", "true", "yes", "on":
		return true
	default:
		return false
	}
}

func dependencyPath(snapshot graphSnapshot) string {
	nodes := []string{
		"Supplier: " + snapshot.SupplierName,
		"Plant: " + snapshot.PlantName,
		"Port: " + snapshot.PortName,
		"Warehouse: " + snapshot.WarehouseName,
		"Product: " + snapshot.ProductID,
		"Alert: " + snapshot.AlertName,
	}
	return strings.Join(nodes, " -> ")
}

func buildHotspotSummary(summary inventoryRiskSummary, hotspots []warehouseHotspot) string {
	destination := pickDestinationHotspot(hotspots)
	source := pickSourceHotspot(hotspots)
	return fmt.Sprintf(
		"Hotspot map for %s: %s in %s, %s is the primary pressure node (score %.2f, coverage %.2f days). %s is the best relief source.",
		summary.ProductID,
		destination.WarehouseName,
		destination.CountyName,
		destination.StateCode,
		destination.HotspotScore,
		destination.CoverageDays,
		source.WarehouseName,
	)
}

func pickDestinationHotspot(hotspots []warehouseHotspot) warehouseHotspot {
	for _, hotspot := range hotspots {
		if strings.Contains(hotspot.RecommendedRole, "DESTINATION") {
			return hotspot
		}
	}
	return hotspots[0]
}

func pickSourceHotspot(hotspots []warehouseHotspot) warehouseHotspot {
	for _, hotspot := range hotspots {
		if strings.Contains(hotspot.RecommendedRole, "SOURCE") {
			return hotspot
		}
	}
	return hotspots[len(hotspots)-1]
}

func seededGraphSnapshots() map[string]graphSnapshot {
	return map[string]graphSnapshot{
		"SKU-500": {
			SupplierName:    "Vertex Plastics",
			TierLevel:       "1",
			SupplierRegion:  "Busan",
			OnTimePct:       "92",
			PlantName:       "Columbus Final Pack",
			CycleDays:       "4.3",
			UtilizationPct:  "78",
			PortName:        "Houston",
			ETAHours:        "54",
			DelayRiskScore:  "0.31",
			WarehouseName:   "Newark Inventory Hub",
			InventoryUnits:  "4226",
			FillRatePct:     "95",
			ProductID:       "SKU-500",
			DemandChangePct: "8",
			MarginPct:       "30",
			AlertName:       "Weather Delay",
			LaneName:        "Pacific lane",
			AlertRisk:       "0.60",
		},
		"SKU-700": {
			SupplierName:    "Atlas Components",
			TierLevel:       "1",
			SupplierRegion:  "Shenzhen",
			OnTimePct:       "96",
			PlantName:       "Austin Assembly",
			CycleDays:       "3.8",
			UtilizationPct:  "84",
			PortName:        "Savannah",
			ETAHours:        "26",
			DelayRiskScore:  "0.14",
			WarehouseName:   "DFW Hub",
			InventoryUnits:  "3180",
			FillRatePct:     "97",
			ProductID:       "SKU-700",
			DemandChangePct: "-3",
			MarginPct:       "26",
			AlertName:       "Customs Hold",
			LaneName:        "Atlantic lane",
			AlertRisk:       "0.28",
		},
		"SKU-900": {
			SupplierName:    "Meridian Metals",
			TierLevel:       "1",
			SupplierRegion:  "Monterrey",
			OnTimePct:       "89",
			PlantName:       "Raleigh Integration",
			CycleDays:       "5.1",
			UtilizationPct:  "73",
			PortName:        "Long Beach",
			ETAHours:        "62",
			DelayRiskScore:  "0.41",
			WarehouseName:   "Chicago Crossdock",
			InventoryUnits:  "2875",
			FillRatePct:     "93",
			ProductID:       "SKU-900",
			DemandChangePct: "11",
			MarginPct:       "22",
			AlertName:       "Capacity Spike",
			LaneName:        "Gulf lane",
			AlertRisk:       "0.44",
		},
	}
}

func seededSummaries() map[string]inventoryRiskSummary {
	return map[string]inventoryRiskSummary{
		"SKU-500": {
			ProductID:                 "SKU-500",
			ProductName:               "Sustainable Widget 500",
			QuarterLabel:              "2026-Q3",
			RiskLevel:                 "HIGH",
			StockoutProbability:       0.72,
			ProjectedRevenueImpactUSD: 185000,
			PrimaryRegion:             "Northeast corridor",
			RecommendationSummary:     "Rebalance inventory into Newark before the next delayed inbound cycle.",
		},
		"SKU-700": {
			ProductID:                 "SKU-700",
			ProductName:               "Low Carbon Kit 700",
			QuarterLabel:              "2026-Q3",
			RiskLevel:                 "MEDIUM",
			StockoutProbability:       0.49,
			ProjectedRevenueImpactUSD: 96000,
			PrimaryRegion:             "Upper Midwest",
			RecommendationSummary:     "Pre-stage buffer inventory in Chicago before demand spikes.",
		},
		"SKU-900": {
			ProductID:                 "SKU-900",
			ProductName:               "Circular Sensor 900",
			QuarterLabel:              "2026-Q3",
			RiskLevel:                 "MEDIUM",
			StockoutProbability:       0.41,
			ProjectedRevenueImpactUSD: 87000,
			PrimaryRegion:             "Southeast",
			RecommendationSummary:     "Maintain current buffers but watch Gulf-port capacity.",
		},
	}
}

func seededHotspots() map[string][]warehouseHotspot {
	return map[string][]warehouseHotspot{
		"SKU-500": {
			{
				ProductID:       "SKU-500",
				WarehouseCode:   "WH-101",
				WarehouseName:   "Newark Inventory Hub",
				CountyName:      "Essex",
				StateCode:       "NJ",
				RegionName:      "Northeast corridor",
				HotspotRank:     1,
				HotspotScore:    0.86,
				CoverageDays:    4.2,
				BacklogUnits:    410,
				ServiceLevelPct: 91.0,
				AtRiskUnits:     620,
				RevenueImpactUSD: 92000,
				RiskLevel:       "CRITICAL",
				RecommendedRole: "DESTINATION_HOTSPOT",
			},
			{
				ProductID:       "SKU-500",
				WarehouseCode:   "WH-202",
				WarehouseName:   "DFW Hub",
				CountyName:      "Tarrant",
				StateCode:       "TX",
				RegionName:      "Southern buffer",
				HotspotRank:     2,
				HotspotScore:    0.31,
				CoverageDays:    11.8,
				BacklogUnits:    120,
				ServiceLevelPct: 97.0,
				AtRiskUnits:     180,
				RevenueImpactUSD: 41000,
				RiskLevel:       "BUFFER",
				RecommendedRole: "SOURCE_BUFFER",
			},
			{
				ProductID:       "SKU-500",
				WarehouseCode:   "WH-303",
				WarehouseName:   "Chicago Crossdock",
				CountyName:      "Cook",
				StateCode:       "IL",
				RegionName:      "Midwest relay",
				HotspotRank:     3,
				HotspotScore:    0.48,
				CoverageDays:    8.1,
				BacklogUnits:    190,
				ServiceLevelPct: 94.0,
				AtRiskUnits:     210,
				RevenueImpactUSD: 52000,
				RiskLevel:       "WATCH",
				RecommendedRole: "RELAY_NODE",
			},
		},
		"SKU-700": {
			{
				ProductID:       "SKU-700",
				WarehouseCode:   "WH-303",
				WarehouseName:   "Chicago Crossdock",
				CountyName:      "Cook",
				StateCode:       "IL",
				RegionName:      "Upper Midwest",
				HotspotRank:     1,
				HotspotScore:    0.74,
				CoverageDays:    5.0,
				BacklogUnits:    360,
				ServiceLevelPct: 92.0,
				AtRiskUnits:     430,
				RevenueImpactUSD: 61000,
				RiskLevel:       "HIGH",
				RecommendedRole: "DESTINATION_HOTSPOT",
			},
			{
				ProductID:       "SKU-700",
				WarehouseCode:   "WH-202",
				WarehouseName:   "DFW Hub",
				CountyName:      "Tarrant",
				StateCode:       "TX",
				RegionName:      "Southern buffer",
				HotspotRank:     2,
				HotspotScore:    0.36,
				CoverageDays:    10.4,
				BacklogUnits:    110,
				ServiceLevelPct: 98.0,
				AtRiskUnits:     170,
				RevenueImpactUSD: 22000,
				RiskLevel:       "BUFFER",
				RecommendedRole: "SOURCE_BUFFER",
			},
			{
				ProductID:       "SKU-700",
				WarehouseCode:   "WH-101",
				WarehouseName:   "Newark Inventory Hub",
				CountyName:      "Essex",
				StateCode:       "NJ",
				RegionName:      "Northeast",
				HotspotRank:     3,
				HotspotScore:    0.29,
				CoverageDays:    12.6,
				BacklogUnits:    90,
				ServiceLevelPct: 96.0,
				AtRiskUnits:     140,
				RevenueImpactUSD: 13000,
				RiskLevel:       "WATCH",
				RecommendedRole: "SATELLITE_NODE",
			},
		},
		"SKU-900": {
			{
				ProductID:       "SKU-900",
				WarehouseCode:   "WH-202",
				WarehouseName:   "DFW Hub",
				CountyName:      "Tarrant",
				StateCode:       "TX",
				RegionName:      "Southeast feeder",
				HotspotRank:     1,
				HotspotScore:    0.63,
				CoverageDays:    6.5,
				BacklogUnits:    280,
				ServiceLevelPct: 93.0,
				AtRiskUnits:     300,
				RevenueImpactUSD: 47000,
				RiskLevel:       "HIGH",
				RecommendedRole: "DESTINATION_HOTSPOT",
			},
			{
				ProductID:       "SKU-900",
				WarehouseCode:   "WH-303",
				WarehouseName:   "Chicago Crossdock",
				CountyName:      "Cook",
				StateCode:       "IL",
				RegionName:      "Midwest relay",
				HotspotRank:     2,
				HotspotScore:    0.34,
				CoverageDays:    11.0,
				BacklogUnits:    115,
				ServiceLevelPct: 97.0,
				AtRiskUnits:     150,
				RevenueImpactUSD: 19000,
				RiskLevel:       "BUFFER",
				RecommendedRole: "SOURCE_BUFFER",
			},
			{
				ProductID:       "SKU-900",
				WarehouseCode:   "WH-101",
				WarehouseName:   "Newark Inventory Hub",
				CountyName:      "Essex",
				StateCode:       "NJ",
				RegionName:      "Northeast",
				HotspotRank:     3,
				HotspotScore:    0.28,
				CoverageDays:    13.2,
				BacklogUnits:    80,
				ServiceLevelPct: 97.0,
				AtRiskUnits:     110,
				RevenueImpactUSD: 12000,
				RiskLevel:       "WATCH",
				RecommendedRole: "SATELLITE_NODE",
			},
		},
	}
}

func seededExternalSignal(productID string) externalSignal {
	switch productID {
	case "SKU-600":
		return externalSignal{
			SignalName:               "Rail congestion watch",
			SignalSummary:            "Midwest rail congestion is extending inbound replenishment by roughly 18 hours.",
			RiskLevel:                "medium",
			RecommendedLeadTimeAction: "Shift transfer timing forward by one day.",
		}
	case "SKU-700":
		return externalSignal{
			SignalName:               "Port labor caution",
			SignalSummary:            "Northeast port labor uncertainty could delay replenishment windows next week.",
			RiskLevel:                "medium",
			RecommendedLeadTimeAction: "Pre-stage transfer inventory before the next inbound delay window.",
		}
	default:
		return externalSignal{
			SignalName:               "Weather delay",
			SignalSummary:            "Pacific lane weather is raising delay risk on the inbound route that feeds Newark.",
			RiskLevel:                "high",
			RecommendedLeadTimeAction: "Prioritize an internal transfer before the next external replenishment cycle.",
		}
	}
}

func (inventoryActionTools) getGraphEvidence(_ tool.Context, input GraphEvidenceArgs) (GraphEvidenceResult, error) {
	productID := normalizeProductID(input.ProductID)
	snapshot, ok := seededGraphSnapshots()[productID]
	if !ok {
		snapshot = seededGraphSnapshots()[defaultProductID]
		snapshot.ProductID = productID
	}
	return GraphEvidenceResult{
		Status:          "ok",
		ProductID:       productID,
		DependencyPath:  dependencyPath(snapshot),
		Warehouse:       "Warehouse: " + snapshot.WarehouseName,
		WarehouseMetric: "Fill rate " + formatPercent(snapshot.FillRatePct),
		ActiveAlert:     "Alert: " + snapshot.AlertName,
		AlertDetail:     snapshot.LaneName,
		AlertMetric:     "Risk " + valueOrDefault(snapshot.AlertRisk, "n/a"),
		SourceMode:      "seeded",
		SourceDetail:    "Seeded supply-chain dependency fallback",
	}, nil
}

func (inventoryActionTools) getSpatialEvidence(_ tool.Context, input SpatialEvidenceArgs) (SpatialEvidenceResult, error) {
	productID := normalizeProductID(input.ProductID)
	summary, ok := seededSummaries()[productID]
	if !ok {
		summary = seededSummaries()[defaultProductID]
		summary.ProductID = productID
	}
	hotspots := append([]warehouseHotspot(nil), seededHotspots()[productID]...)
	if len(hotspots) == 0 {
		hotspots = append([]warehouseHotspot(nil), seededHotspots()[defaultProductID]...)
	}
	sort.Slice(hotspots, func(i, j int) bool {
		return hotspots[i].HotspotRank < hotspots[j].HotspotRank
	})
	destination := pickDestinationHotspot(hotspots)
	source := pickSourceHotspot(hotspots)
	return SpatialEvidenceResult{
		Status:                         "ok",
		ProductID:                      productID,
		HotspotRegion:                  summary.PrimaryRegion,
		HotspotSummary:                 buildHotspotSummary(summary, hotspots),
		RecommendedSourceWarehouse:     "Warehouse: " + source.WarehouseName,
		RecommendedDestinationWarehouse: "Warehouse: " + destination.WarehouseName,
		SuggestedTransferUnits:         500,
		CoverageRiskDays:               destination.CoverageDays,
		SourceDetail:                   "Seeded spatial hotspot fallback",
	}, nil
}

func (inventoryActionTools) getExternalSignals(_ tool.Context, input ExternalSignalsArgs) (ExternalSignalsResult, error) {
	productID := normalizeProductID(input.ProductID)
	signal := seededExternalSignal(productID)
	return ExternalSignalsResult{
		Status:                    "ok",
		ProductID:                 productID,
		SignalName:                signal.SignalName,
		SignalSummary:             signal.SignalSummary,
		RiskLevel:                 signal.RiskLevel,
		RecommendedLeadTimeAction: signal.RecommendedLeadTimeAction,
		ObservedDate:              time.Now().Format("2006-01-02"),
		SourceDetail:              "Seeded external signal summary",
	}, nil
}

func (inventoryActionTools) checkTransferPolicy(_ tool.Context, input TransferPolicyArgs) (TransferPolicyResult, error) {
	productID := normalizeProductID(input.ProductID)
	sameWarehouse := strings.EqualFold(strings.TrimSpace(input.SourceWarehouse), strings.TrimSpace(input.DestinationWarehouse))
	requiresApproval := input.Units >= 400 || sameWarehouse
	allowed := !sameWarehouse && input.Units > 0
	policySummary := "Transfer is blocked because source and destination warehouses are the same or units are missing."
	if allowed && requiresApproval {
		policySummary = "Transfer can be drafted but requires approval before execution."
	} else if allowed {
		policySummary = "Transfer can be drafted immediately with standard review."
	}
	return TransferPolicyResult{
		Status:                    map[bool]string{true: "ok", false: "blocked"}[allowed],
		ProductID:                 productID,
		Allowed:                   allowed,
		RequiresApproval:          requiresApproval,
		PolicySummary:             policySummary,
		UnitsThresholdForApproval: 400,
		Reason:                    valueOrDefault(input.Reason, "No reason supplied"),
	}, nil
}

func (inventoryActionTools) draftInventoryTransferAction(_ tool.Context, input DraftInventoryTransferArgs) (DraftInventoryTransferResult, error) {
	productID := normalizeProductID(input.ProductID)
	approvalState := "STANDARD_REVIEW"
	if input.Units >= 400 {
		approvalState = "PENDING_APPROVAL"
	}
	return DraftInventoryTransferResult{
		Status:               "drafted",
		ActionType:           "INVENTORY_TRANSFER",
		DraftActionID:        "draft-transfer-" + strings.ToLower(productID),
		ProductID:            productID,
		SourceWarehouse:      valueOrDefault(input.SourceWarehouse, "Unknown source"),
		DestinationWarehouse: valueOrDefault(input.DestinationWarehouse, "Unknown destination"),
		Units:                input.Units,
		Reason:               valueOrDefault(input.Reason, "No reason supplied"),
		ExecutionState:       "NOT_EXECUTED",
		ApprovalState:        approvalState,
	}, nil
}

func (c *inventoryActionCoordinator) run(userInput string, contextID string) InventoryActionResult {
	normalizedInput := strings.TrimSpace(userInput)
	if normalizedInput == "" {
		normalizedInput = "Recommend an inventory action for SKU-500."
	}
	if !containsProductID(normalizedInput) {
		normalizedInput += "\nUse SKU-500 as the default product id when the request does not specify one."
	}
	return c.runDeterministicFallback(normalizedInput, "Go fallback orchestration is serving this request.")
}

func (c *inventoryActionCoordinator) runDeterministicFallback(userInput string, reason string) InventoryActionResult {
	tools := inventoryActionTools{}
	productID := extractProductID(userInput)

	graphEvidence, _ := tools.getGraphEvidence(nil, GraphEvidenceArgs{ProductID: productID})
	spatialEvidence, _ := tools.getSpatialEvidence(nil, SpatialEvidenceArgs{ProductID: productID})
	externalSignals, _ := tools.getExternalSignals(nil, ExternalSignalsArgs{ProductID: productID})

	moveReason := combinedReason(graphEvidence.ActiveAlert, externalSignals.SignalSummary)
	policyResult, _ := tools.checkTransferPolicy(nil, TransferPolicyArgs{
		ProductID:            productID,
		SourceWarehouse:      spatialEvidence.RecommendedSourceWarehouse,
		DestinationWarehouse: spatialEvidence.RecommendedDestinationWarehouse,
		Units:                spatialEvidence.SuggestedTransferUnits,
		Reason:               moveReason,
	})
	draftResult, _ := tools.draftInventoryTransferAction(nil, DraftInventoryTransferArgs{
		ProductID:            productID,
		SourceWarehouse:      spatialEvidence.RecommendedSourceWarehouse,
		DestinationWarehouse: spatialEvidence.RecommendedDestinationWarehouse,
		Units:                spatialEvidence.SuggestedTransferUnits,
		Reason:               moveReason,
	})

	approvalLine := "Only standard review is required before execution."
	if policyResult.RequiresApproval {
		approvalLine = "Approval is required before execution."
	}

	responseText := fmt.Sprintf(
		"Fallback recommendation for %s: transfer %d units from %s to %s. Why: %s %s %s %s Draft action id: %s. Policy check: %s The ADK model path was unavailable, so this response used deterministic local orchestration instead (%s).",
		productID,
		spatialEvidence.SuggestedTransferUnits,
		spatialEvidence.RecommendedSourceWarehouse,
		spatialEvidence.RecommendedDestinationWarehouse,
		toSentence(graphEvidence.DependencyPath),
		toSentence(graphEvidence.ActiveAlert),
		toSentence(spatialEvidence.HotspotSummary),
		toSentence(externalSignals.SignalSummary)+" "+toSentence(approvalLine),
		draftResult.DraftActionID,
		toSentence(policyResult.PolicySummary),
		toParenthetical(reason),
	)

	trace := []string{
		mustJSON("graphEvidence", graphEvidence),
		mustJSON("spatialEvidence", spatialEvidence),
		mustJSON("externalSignals", externalSignals),
		mustJSON("policyResult", policyResult),
		mustJSON("draftResult", draftResult),
	}

	return InventoryActionResult{
		ResponseText:      responseText,
		Trace:             trace,
		OrchestrationMode: "deterministic-fallback",
		DraftAction: map[string]any{
			"status":               draftResult.Status,
			"actionType":           draftResult.ActionType,
			"draftActionId":        draftResult.DraftActionID,
			"productId":            draftResult.ProductID,
			"sourceWarehouse":      draftResult.SourceWarehouse,
			"destinationWarehouse": draftResult.DestinationWarehouse,
			"units":                draftResult.Units,
			"reason":               draftResult.Reason,
			"executionState":       draftResult.ExecutionState,
			"approvalState":        draftResult.ApprovalState,
		},
		PolicyResult: map[string]any{
			"status":                    policyResult.Status,
			"productId":                 policyResult.ProductID,
			"allowed":                   policyResult.Allowed,
			"requiresApproval":          policyResult.RequiresApproval,
			"policySummary":             policyResult.PolicySummary,
			"unitsThresholdForApproval": policyResult.UnitsThresholdForApproval,
			"reason":                    policyResult.Reason,
		},
	}
}

func mustJSON(label string, value any) string {
	body, err := json.Marshal(value)
	if err != nil {
		return label + "=<marshal-error>"
	}
	return label + "=" + string(body)
}

func (c *inventoryActionCoordinator) buildRootAgent(ctx context.Context) (agent.Agent, error) {
	if truthy(c.env["ACTION_DISABLE_ADK"]) {
		return c.buildDeterministicAgent(), nil
	}

	modelName := firstNonBlank(c.env["ACTION_COORDINATOR_MODEL"], c.env["MODEL_NAME"], defaultModelName)
	modelConfig := &genai.ClientConfig{}
	project := firstNonBlank(c.env["GOOGLE_CLOUD_PROJECT"], c.env["GCP_PROJECT_ID"])
	location := firstNonBlank(c.env["GOOGLE_CLOUD_LOCATION"], c.env["GOOGLE_CLOUD_REGION"], c.env["GCP_REGION"])
	if apiKey := firstNonBlank(c.env["GOOGLE_API_KEY"], c.env["GEMINI_API_KEY"]); apiKey != "" {
		modelConfig.APIKey = apiKey
		modelConfig.Backend = genai.BackendGeminiAPI
	}
	if project != "" {
		modelConfig.Project = project
	}
	if location != "" {
		modelConfig.Location = location
	}
	if modelConfig.APIKey == "" && modelConfig.Project != "" && modelConfig.Location != "" {
		modelConfig.Backend = genai.BackendVertexAI
	}

	model, err := gemini.NewModel(ctx, modelName, modelConfig)
	if err != nil {
		log.Printf("Falling back to deterministic Go agent because Gemini model init failed: %v", err)
		return c.buildDeterministicAgent(), nil
	}

	tools := inventoryActionTools{}
	graphTool, err := functiontool.New(functiontool.Config{
		Name:        "getGraphEvidence",
		Description: "Look up Oracle supply-chain dependency evidence for a product and summarize the supplier path, alert, and warehouse involved.",
	}, tools.getGraphEvidence)
	if err != nil {
		return nil, fmt.Errorf("failed to create getGraphEvidence tool: %w", err)
	}
	spatialTool, err := functiontool.New(functiontool.Config{
		Name:        "getSpatialEvidence",
		Description: "Return a seeded hotspot summary for a product, including suggested source and destination warehouses for a balancing move.",
	}, tools.getSpatialEvidence)
	if err != nil {
		return nil, fmt.Errorf("failed to create getSpatialEvidence tool: %w", err)
	}
	externalTool, err := functiontool.New(functiontool.Config{
		Name:        "getExternalSignals",
		Description: "Return a seeded external-signals summary, such as weather or geopolitical risk, for a product's supply lane.",
	}, tools.getExternalSignals)
	if err != nil {
		return nil, fmt.Errorf("failed to create getExternalSignals tool: %w", err)
	}
	policyTool, err := functiontool.New(functiontool.Config{
		Name:        "checkTransferPolicy",
		Description: "Check whether a proposed inventory move is allowed immediately or requires approval based on unit volume and route choice.",
	}, tools.checkTransferPolicy)
	if err != nil {
		return nil, fmt.Errorf("failed to create checkTransferPolicy tool: %w", err)
	}
	draftTool, err := functiontool.New(functiontool.Config{
		Name:        "draftInventoryTransferAction",
		Description: "Create a draft inventory-transfer action recommendation. This does not execute the move.",
	}, tools.draftInventoryTransferAction)
	if err != nil {
		return nil, fmt.Errorf("failed to create draftInventoryTransferAction tool: %w", err)
	}

	graphAgent, err := llmagent.New(llmagent.Config{
		Name:        "graph_evidence_specialist",
		Description: "Oracle Graph specialist for supply-chain dependency evidence.",
		Model:       model,
		Instruction: "You are the graph-evidence specialist for inventory risk response.\nAlways call getGraphEvidence for the relevant productId before answering.\nReturn only a concise supply-chain evidence summary and never recommend an action.",
		Tools:       []tool.Tool{graphTool},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create graph evidence agent: %w", err)
	}
	spatialAgent, err := llmagent.New(llmagent.Config{
		Name:        "spatial_evidence_specialist",
		Description: "Spatial hotspot specialist for warehouse pressure and transfer direction.",
		Model:       model,
		Instruction: "You are the spatial-evidence specialist for inventory risk response.\nAlways call getSpatialEvidence for the relevant productId before answering.\nReturn only a concise hotspot summary with recommended source and destination warehouses.\nDo not make a final action recommendation.",
		Tools:       []tool.Tool{spatialTool},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create spatial evidence agent: %w", err)
	}
	externalAgent, err := llmagent.New(llmagent.Config{
		Name:        "external_signal_specialist",
		Description: "External-risk specialist for weather and geopolitical supply-lane impacts.",
		Model:       model,
		Instruction: "You are the external-signals specialist for inventory risk response.\nAlways call getExternalSignals for the relevant productId before answering.\nReturn only a concise summary of outside factors that could change the timing or urgency of an action.\nDo not make a final action recommendation.",
		Tools:       []tool.Tool{externalTool},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create external signals agent: %w", err)
	}

	parallelEvidenceAgent, err := parallelagent.New(parallelagent.Config{
		AgentConfig: agent.Config{
			Name:        "parallel_evidence_gatherer",
			Description: "Runs graph, spatial, and external-signal specialists in parallel before an action recommendation is made.",
			SubAgents:   []agent.Agent{graphAgent, spatialAgent, externalAgent},
		},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create parallel evidence agent: %w", err)
	}

	decisionAgent, err := llmagent.New(llmagent.Config{
		Name:        "inventory_action_decider",
		Description: "Synthesizes evidence, checks policy, and drafts an inventory action recommendation.",
		Model:       model,
		Instruction: "You are the final inventory-action coordinator.\nReview the graph, spatial, and external evidence already gathered in this session.\nYour job is to recommend one next step: transfer, expedite, substitute, or hold.\nIf a transfer is the best next move, call checkTransferPolicy first and then call draftInventoryTransferAction.\nNever claim that an inventory move has been executed.\nYour final answer must include:\n1. Recommended action.\n2. Why that action is justified from the evidence.\n3. Whether approval is required.\n4. If you drafted a move, the draft action id and the proposed source, destination, and units.\nIf evidence is missing, say so plainly and recommend the safest next step.",
		Tools:       []tool.Tool{policyTool, draftTool},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create decision agent: %w", err)
	}

	rootAgent, err := sequentialagent.New(sequentialagent.Config{
		AgentConfig: agent.Config{
			Name:        "inventory_action_orchestrator",
			Description: "Coordinates final-stage inventory action planning using evidence specialists and a decision agent.",
			SubAgents:   []agent.Agent{parallelEvidenceAgent, decisionAgent},
		},
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create root sequential agent: %w", err)
	}
	return rootAgent, nil
}

func (c *inventoryActionCoordinator) buildDeterministicAgent() agent.Agent {
	custom, err := agent.New(agent.Config{
		Name:        "inventory_action_fallback_agent",
		Description: "Deterministic fallback inventory action coordinator.",
		Run: func(ctx agent.InvocationContext) iter.Seq2[*session.Event, error] {
			return func(yield func(*session.Event, error) bool) {
				userContent := ctx.UserContent()
				userInput := ""
				if userContent != nil {
					for _, part := range userContent.Parts {
						userInput += strings.TrimSpace(part.Text) + "\n"
					}
					userInput = strings.TrimSpace(userInput)
				}
				result := c.run(userInput, ctx.InvocationID())
				response := genai.NewContentFromText(result.ResponseText, genai.RoleModel)
				event := session.NewEvent(ctx.InvocationID())
				event.Author = "inventory_action_fallback_agent"
				event.Content = response
				if !yield(event, nil) {
					return
				}
			}
		},
	})
	if err != nil {
		panic(err)
	}
	return custom
}
