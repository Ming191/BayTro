#!/usr/bin/env python3
"""
Test script cho GraphRAG FastAPI server
"""

import requests
import json
import time

BASE_URL = "http://localhost:5000"

def test_health():
    """Test health endpoint"""
    print("Testing health endpoint...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        if response.status_code == 200:
            data = response.json()
            print(f"Health check passed: {data}")
            return True
        else:
            print(f"Health check failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"Health check error: {e}")
        return False

def test_query():
    """Test query endpoint"""
    print("\nTesting query endpoint...")
    try:
        payload = {
            "question": "Quyền và nghĩa vụ của người thuê nhà là gì?",
            "k": 3,
            "expand_depth": 2
        }
        
        response = requests.post(
            f"{BASE_URL}/query",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            data = response.json()
            print(f"Query successful!")
            print(f"Question: {data['question']}")
            print(f"Answer: {data['answer'][:200]}...")
            print(f"Context length: {len(data['context'])} characters")
            return True
        else:
            print(f"Query failed: {response.status_code}")
            print(f"Error: {response.text}")
            return False
    except Exception as e:
        print(f"Query error: {e}")
        return False

def test_search():
    """Test search endpoint"""
    print("\nTesting search endpoint...")
    try:
        payload = {
            "query": "thuê nhà",
            "top_k": 3
        }
        
        response = requests.post(
            f"{BASE_URL}/search",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            data = response.json()
            print(f"Search successful!")
            print(f"Query: {data['query']}")
            print(f"Found {len(data['results'])} results")
            for i, result in enumerate(data['results'][:2]):
                print(f"  {i+1}. {result['id']} (score: {result['score']:.3f})")
            return True
        else:
            print(f"Search failed: {response.status_code}")
            print(f"Error: {response.text}")
            return False
    except Exception as e:
        print(f" Search error: {e}")
        return False

def test_invalid_request():
    """Test invalid request handling"""
    print("\nTesting invalid request handling...")
    try:
        # Test empty question
        payload = {"question": ""}
        response = requests.post(f"{BASE_URL}/query", json=payload)
        if response.status_code == 422:  # Validation error
            print(" Empty question validation works")
        else:
            print(f" Empty question validation failed: {response.status_code}")
        
        # Test missing field
        payload = {"invalid_field": "test"}
        response = requests.post(f"{BASE_URL}/query", json=payload)
        if response.status_code == 422:  # Validation error
            print("Missing field validation works")
        else:
            print(f"Missing field validation failed: {response.status_code}")
            
        return True
    except Exception as e:
        print(f"Invalid request test error: {e}")
        return False

def main():
    """Run all tests"""
    print("Starting GraphRAG API tests...")
    print(f"Testing server at: {BASE_URL}")
    print("=" * 50)
    
    # Wait a bit for server to be ready
    time.sleep(2)
    
    tests = [
        test_health,
        test_query,
        test_search,
        test_invalid_request
    ]
    
    passed = 0
    total = len(tests)
    
    for test in tests:
        if test():
            passed += 1
    
    print("\n" + "=" * 50)
    print(f"Test Results: {passed}/{total} tests passed")
    
    if passed == total:
        print("All tests passed! API is working correctly.")
    else:
        print("Some tests failed. Check server logs for details.")
    
    print(f"\n📖 API Documentation available at: {BASE_URL}/docs")

if __name__ == "__main__":
    main()
