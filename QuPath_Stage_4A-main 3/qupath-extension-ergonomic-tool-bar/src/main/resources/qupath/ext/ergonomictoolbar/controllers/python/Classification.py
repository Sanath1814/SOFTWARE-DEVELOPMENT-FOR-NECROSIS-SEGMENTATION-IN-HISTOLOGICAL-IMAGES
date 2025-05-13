import os
import sys
import zipfile
from PIL import Image
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from torchvision import models
import numpy as np
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Tuple, Dict, Union

# Set random seeds for reproducibility
np.random.seed(0)
torch.manual_seed(0)
torch.backends.cudnn.deterministic = True
torch.backends.cudnn.benchmark = False

# Define the transformation for the images
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])

# Load the pre-trained model
model = models.resnet18(weights=None)
model.fc = nn.Linear(model.fc.in_features, 3)  # 3 classes for classification
model_path = sys.argv[2] + "/model.pth"
model.load_state_dict(torch.load(model_path, map_location=torch.device('cpu')))
model.eval()


def load_image(image: Image.Image) -> torch.Tensor:
    """
    Load and transform an image for model prediction.

    Parameters:
    - image (PIL.Image.Image): The image to transform.

    Returns:
    torch.Tensor: The transformed image tensor.
    """
    image = transform(image)
    image = image.unsqueeze(0)  # Add a batch dimension
    return image


def predict_image(image: Image.Image) -> int:
    """
    Predict the class of an image using the pre-trained model.

    Parameters:
    - image (PIL.Image.Image): The image to predict.

    Returns:
    int: The predicted class (0, 1, or 2).
    """
    image = load_image(image)

    with torch.no_grad():
        output = model(image)
        _, predicted = torch.max(output, 1)

    return predicted.item()


def process_image(image_path: str) -> Tuple[str, Union[int, str]]:
    """
    Process an image and predict its class.

    Parameters:
    - image_path (str): The path to the image.

    Returns:
    tuple: The image path and the predicted class or an error message.
    """
    try:
        image = Image.open(image_path).convert('RGB')
        _pred = predict_image(image)
        return image_path, _pred

    except Exception as e:
        return image_path, str(e)


def process_zip(_zip_path: str) -> Dict[str, Union[int, str]]:
    """
    Extract and process images from a zip file, predicting their classes.

    Parameters:
    - zip_path (str): The path to the zip file.

    Returns:
    dict: A dictionary mapping image file names to their predicted classes or
          error messages.
    """
    # Extract the files from the zip
    with zipfile.ZipFile(_zip_path, 'r') as zip_ref:
        zip_ref.extractall('extracted_images')

    image_paths = []
    for root, _, files in os.walk('extracted_images'):
        for file in files:
            if file.endswith('.tif') and file.lower().startswith('t'):
                image_path = os.path.join(root, file)
                image_paths.append(image_path)

    predictions = {}

    # Use ThreadPoolExecutor to process images in parallel
    with ThreadPoolExecutor() as executor:
        futures = [executor.submit(process_image, *image_path) for image_path in
                   image_paths]

        for future in as_completed(futures):
            image_path, pred = future.result()
            file_name = os.path.basename(image_path)
            predictions[file_name] = pred

    return predictions


def main() -> None:
    """
    Main function to execute the script.
    """
    # We check that all arguments are present
    if len(sys.argv) != 3:
        print("Usage: python classify_images.py <zip_path>")
        sys.exit(1)

    zip_path = sys.argv[1]

    # We check the file exist
    if not os.path.isfile(zip_path):
        print(f"The file {zip_path} does not exist.")
        sys.exit(1)

    results = process_zip(zip_path)

    # We print the results
    for filename, pred in results.items():
        print(f"{filename}: {pred}")


if __name__ == "__main__":
    main()
