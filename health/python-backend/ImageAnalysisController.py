import oci
from oci.ai_vision import AIServiceVisionClient
from oci.ai_vision.models import AnalyzeImageDetails

from flask import Flask, request, jsonify

import logging

app = Flask(__name__)

# add logger
log = logging.getLogger(__name__)

OCI_CONFIG = oci.config.from_file()

@app.route("/imageanalysis/analyzeimage", methods = ["POST"])
def analyzeimage():

    json_data = request.json

    required_params = ['COMPARMENT_OCID', 'namespace_name', 'bucket_name', 'object_name']
    for param in required_params:
        if param not in json_data:
            return jsonify({'error': f'Missing required parameter: {param}'}), 400
    
    ai_vision_client = oci.ai_vision.AIServiceVisionClient(OCI_CONFIG)
    analyze_image_response = ai_vision_client.analyze_image(
        analyze_image_details=oci.ai_vision.models.AnalyzeImageDetails(
            features=[
                oci.ai_vision.models.ImageObjectDetectionFeature(
                    feature_type="OBJECT_DETECTION",
                    max_results=130,
                    #TODO: add model_id
                    # model_id=""
                    )],
            image=oci.ai_vision.models.ObjectStorageImageDetails(
                source="OBJECT_STORAGE",
                namespace_name=json_data['namespace_name'],
                bucket_name=json_data['bucket_name'],
                object_name=json_data['object_name']),
            compartment_id=json_data['COMPARMENT_OCID']))
        
    image_objects = analyze_image_response.data.image_objects
    if image_objects == None:
        raise Exception("Failed to detect objects from image.")
    
    detected_objects = []

    for image_object in image_objects:
        detected_objects.append({"name": image_object.name, "confidence":image_object.confidence})

    return detected_objects

if __name__ == '__main__':
    app.run(debug=True)
