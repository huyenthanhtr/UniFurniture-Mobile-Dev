const fetch = require("node-fetch");

async function run() {
  const url = "https://unifur-git-454662189896.europe-west1.run.app/api/cart/items/upsert";
  const body = {
    cart_id: "69c0c6108b9eb1b93a83d6b0",
    variant_id: "69a6c3f0435745d9034c4c8e",
    quantity: 1
  };

  console.log("Sending request to deployed server:", url);
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    console.log("Status:", res.status);
    const json = await res.json();
    console.log("Response JSON:", JSON.stringify(json, null, 2));
  } catch (err) {
    console.error(err);
  }
}

run();
