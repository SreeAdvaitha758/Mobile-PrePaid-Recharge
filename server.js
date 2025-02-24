const express = require("express");
const fs = require("fs");
const path = require("path");
const bodyParser = require("body-parser");

const app = express();
const PORT = 3000;

// Middleware to parse JSON data
app.use(bodyParser.json());

// Serve static files (e.g., your HTML, CSS, JS)
app.use(express.static("public"));

// Path to the plans.json file
const plansFilePath = path.join(__dirname, "User", "plans.json");

// Helper function to read plans from the JSON file
function readPlans() {
    const data = fs.readFileSync(plansFilePath, "utf8");
    return JSON.parse(data);
}

// Helper function to write plans to the JSON file
function writePlans(plans) {
    fs.writeFileSync(plansFilePath, JSON.stringify(plans, null, 2), "utf8");
}

// Fetch all plans
app.get("/plans", (req, res) => {
    try {
        const plans = readPlans();
        res.json(plans);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch plans." });
    }
});

// Add a new plan
app.post("/plans", (req, res) => {
    const { planName, planPrice, planCategory } = req.body;

    if (!planName || !planPrice || !planCategory) {
        return res.status(400).json({ error: "All fields are required." });
    }

    try {
        const plans = readPlans();

        const newPlan = {
            price: `₹${planPrice}`,
            validity: "28 Days", // Default validity
            data: "1GB/Day", // Default data
            description: planName,
        };

        // Add the new plan to the appropriate category
        if (!plans[planCategory]) {
            plans[planCategory] = [];
        }
        plans[planCategory].push(newPlan);

        // Save the updated plans to the JSON file
        writePlans(plans);

        res.json({ message: "Plan added successfully!", plans });
    } catch (error) {
        res.status(500).json({ error: "Failed to add plan." });
    }
});

// Edit a plan
app.put("/plans/:category/:index", (req, res) => {
    const { category, index } = req.params;
    const { planName, planPrice } = req.body;

    if (!planName || !planPrice) {
        return res.status(400).json({ error: "All fields are required." });
    }

    try {
        const plans = readPlans();

        if (!plans[category] || !plans[category][index]) {
            return res.status(404).json({ error: "Plan not found." });
        }

        // Update the plan
        plans[category][index].description = planName;
        plans[category][index].price = `₹${planPrice}`;

        // Save the updated plans to the JSON file
        writePlans(plans);

        res.json({ message: "Plan updated successfully!", plans });
    } catch (error) {
        res.status(500).json({ error: "Failed to update plan." });
    }
});

// Delete a plan
app.delete("/plans/:category/:index", (req, res) => {
    const { category, index } = req.params;

    try {
        const plans = readPlans();

        if (!plans[category] || !plans[category][index]) {
            return res.status(404).json({ error: "Plan not found." });
        }

        // Remove the plan
        plans[category].splice(index, 1);

        // Save the updated plans to the JSON file
        writePlans(plans);

        res.json({ message: "Plan deleted successfully!", plans });
    } catch (error) {
        res.status(500).json({ error: "Failed to delete plan." });
    }
});

// Start the server
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});